package com.funcode.academy.runner

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import com.funcode.academy.domain.runner.CodeRunner
import com.funcode.academy.domain.runner.Limit
import com.funcode.academy.domain.runner.ProgramEvaluation
import com.funcode.academy.domain.runner.RunRequest
import com.funcode.academy.domain.runner.RunResult
import com.funcode.academy.domain.runner.RunnerCapabilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * PROTOTIPO (Fase 8). Cliente de [JavaRunnerService]: cada ejecución arranca un proceso
 * aislado nuevo, le envía el código y espera el resultado. Nada se ejecuta en el proceso
 * de la app ni en el hilo de la interfaz.
 *
 * Recuperación: si el proceso muere (memoria agotada, fallo), la app recibe la
 * desconexión y devuelve un resultado controlado; la siguiente ejecución usa otro proceso.
 */
class IsolatedJavaRunner(context: Context) : CodeRunner {

    private val appContext = context.applicationContext
    private val mutex = Mutex()

    override val capabilities = RunnerCapabilities(canExecute = true)

    override suspend fun run(request: RunRequest): RunResult = runWithDiagnostics(request, null)

    /** Igual que [run]; `diagnostics` solo lo usan las pruebas en compilaciones de depuración. */
    suspend fun runWithDiagnostics(request: RunRequest, diagnostics: Bundle?): RunResult = mutex.withLock {
        ProgramEvaluation.precheck(request)?.let { return it }
        repeat(MAX_ATTEMPTS - 1) {
            val r = runOnce(request, diagnostics)
            if (!(r is RunResult.Unavailable && r.reason == RETRY)) return r
            delay(RETRY_DELAY_MS)
        }
        val last = runOnce(request, diagnostics)
        if (last is RunResult.Unavailable && last.reason == RETRY) RunResult.Unavailable("El proceso de ejecución no está disponible.") else last
    }

    private suspend fun runOnce(request: RunRequest, diagnostics: Bundle?): RunResult {
        val replyThread = HandlerThread("funcode-runner-cliente").apply { start() }
        val result = CompletableDeferred<RunResult>()
        val connected = CompletableDeferred<IBinder>()
        val disconnected = CompletableDeferred<Unit>()
        val replies = Messenger(Handler(replyThread.looper) { msg ->
            if (msg.what == RunnerProtocol.MSG_RESULT) {
                val r = RunnerProtocol.decodeResult(msg.data)
                result.complete(if (r is RunResult.Unavailable && r.reason == JavaRunnerService.BUSY) RunResult.Unavailable(RETRY) else r)
            }
            true
        })
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                connected.complete(service)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                disconnected.complete(Unit)
            }

            override fun onBindingDied(name: ComponentName) {
                disconnected.complete(Unit)
            }
        }
        val intent = Intent(appContext, JavaRunnerService::class.java)
        if (!appContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            replyThread.quitSafely()
            return RunResult.Unavailable("No se pudo iniciar el proceso de ejecución.")
        }
        try {
            val binder = withTimeoutOrNull(START_TIMEOUT_MS) { connected.await() }
                ?: return RunResult.Unavailable("El proceso de ejecución no arrancó a tiempo.")
            val message = Message.obtain(null, RunnerProtocol.MSG_RUN).apply {
                data = RunnerProtocol.encodeRequest(request, diagnostics)
                replyTo = replies
            }
            try {
                Messenger(binder).send(message)
            } catch (e: RemoteException) {
                return RunResult.Unavailable(RETRY)
            }
            val deadline = request.limits.compileTimeoutMs + request.limits.timeoutMs + CLIENT_MARGIN_MS
            val outcome: RunResult = withTimeoutOrNull(deadline) {
                select<RunResult> {
                    result.onAwait { it }
                    disconnected.onAwait {
                        // El proceso termina justo después de responder: se espera un poco por si
                        // la respuesta todavía está en camino. Si no llega, el proceso murió.
                        val late: RunResult? = withTimeoutOrNull(REPLY_GRACE_MS) { result.await() }
                        late ?: RunResult.LimitExceeded(Limit.MEMORY, "")
                    }
                }
            } ?: RunResult.Unavailable("El proceso de ejecución no respondió a tiempo.")
            // Se espera a que el proceso termine para que la siguiente ejecución empiece en uno nuevo.
            withTimeoutOrNull(PROCESS_EXIT_WAIT_MS) { disconnected.await() }
            return outcome
        } catch (e: CancellationException) {
            // El estudiante pulsó «Detener»: se pide al proceso que termine de inmediato.
            withContext(NonCancellable) {
                if (connected.isCompleted) {
                    try {
                        Messenger(connected.await()).send(Message.obtain(null, RunnerProtocol.MSG_CANCEL))
                    } catch (_: RemoteException) {
                        // Ya había terminado.
                    }
                    withTimeoutOrNull(PROCESS_EXIT_WAIT_MS) { disconnected.await() }
                }
            }
            throw e
        } finally {
            try {
                appContext.unbindService(connection)
            } catch (_: IllegalArgumentException) {
                // Ya desvinculado.
            }
            replyThread.quitSafely()
        }
    }

    private companion object {
        const val RETRY = "funcode.retry"
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 300L
        const val START_TIMEOUT_MS = 10_000L
        const val CLIENT_MARGIN_MS = 5_000L
        const val REPLY_GRACE_MS = 500L
        const val PROCESS_EXIT_WAIT_MS = 2_000L
    }
}
