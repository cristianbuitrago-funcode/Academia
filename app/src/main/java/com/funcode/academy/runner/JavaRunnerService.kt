package com.funcode.academy.runner

import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.Process
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import com.funcode.academy.domain.runner.Limit
import com.funcode.academy.domain.runner.ProgramEvaluation
import com.funcode.academy.domain.runner.RunResult
import com.funcode.academy.domain.runner.RunTimings
import com.funcode.academy.javarunner.AllowlistClassLoader
import com.funcode.academy.javarunner.JavaSourceCompiler
import com.funcode.academy.javarunner.ProgramExecutor
import com.funcode.academy.javarunner.StubLibrary
import dalvik.system.InMemoryDexClassLoader
import java.nio.ByteBuffer

/**
 * PROTOTIPO (Fase 8). Compila y ejecuta UN programa y termina su propio proceso.
 *
 * Se declara con `android:isolatedProcess="true"`: corre con un UID propio, sin permisos
 * y sin acceso a los archivos de la app (base de datos, preferencias). Terminar el proceso
 * tras cada ejecución garantiza que un bucle infinito o un estado estático no sobreviven.
 */
class JavaRunnerService : Service() {

    private val thread = HandlerThread("funcode-runner").apply { start() }
    private val messenger = Messenger(Handler(thread.looper) { handle(it); true })

    @Volatile private var used = false

    override fun onBind(intent: Intent): IBinder = messenger.binder

    private fun handle(msg: Message) {
        when (msg.what) {
            RunnerProtocol.MSG_CANCEL -> terminate()
            RunnerProtocol.MSG_RUN -> {
                val replyTo = msg.replyTo ?: return
                if (used) {
                    // Un proceso por ejecución: nunca se reutiliza.
                    reply(replyTo, RunResult.Unavailable(BUSY))
                    return
                }
                used = true
                val data = Bundle(msg.data)
                Thread({ runAndReply(data, replyTo) }, "funcode-pipeline").start()
            }
        }
    }

    private fun runAndReply(data: Bundle, replyTo: Messenger) {
        val request = RunnerProtocol.decodeRequest(data)
        // Vigilante: si compilar o convertir a DEX se bloquea, se responde y se termina igualmente.
        val deadline = request.limits.compileTimeoutMs + request.limits.timeoutMs + WATCHDOG_MARGIN_MS
        Thread({
            SystemClock.sleep(deadline)
            reply(replyTo, RunResult.LimitExceeded(Limit.TIME, ""))
            terminate()
        }, "funcode-vigilante").apply { isDaemon = true }.start()

        val result = try {
            execute(request, data)
        } catch (t: Throwable) {
            Log.e(TAG, "Fallo del ejecutor", t)
            RunResult.Unavailable("Fallo del ejecutor: ${t.javaClass.name}: ${t.message}")
        }
        reply(replyTo, result)
        terminate()
    }

    private fun execute(request: com.funcode.academy.domain.runner.RunRequest, data: Bundle): RunResult {
        ProgramEvaluation.precheck(request)?.let { return it }
        val start = SystemClock.elapsedRealtime()
        val diagnostics = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val extraLibrary = if (diagnostics) data.getStringArrayList(RunnerProtocol.DIAG_EXTRA_LIBRARY).orEmpty() else emptyList()
        val unrestricted = diagnostics && data.getBoolean(RunnerProtocol.DIAG_UNRESTRICTED_LOADER)

        val stubs = StubLibrary.load(javaClass.classLoader!!)
        val compiled = JavaSourceCompiler(stubs).compile(request.source, extraLibrary)
        val compileMs = SystemClock.elapsedRealtime() - start
        if (compiled is JavaSourceCompiler.Outcome.Failure) {
            return RunResult.CompilationFailed(compiled.diagnostics, RunTimings(compileMs = compileMs, totalMs = compileMs))
        }
        compiled as JavaSourceCompiler.Outcome.Success

        val dexStart = SystemClock.elapsedRealtime()
        val dex = D8Dexer.dex(compiled.classes, stubs)
        val dexMs = SystemClock.elapsedRealtime() - dexStart

        val parent = if (unrestricted) Any::class.java.classLoader else AllowlistClassLoader(stubs.allowedClassNames)
        val loader = InMemoryDexClassLoader(ByteBuffer.wrap(dex), parent)
        val run = ProgramExecutor.execute(loader, compiled.classes.keys, request.limits.timeoutMs, request.limits.maxOutputChars)
        val timings = RunTimings(compileMs, dexMs, run.timings.executeMs, SystemClock.elapsedRealtime() - start)
        return when (run) {
            is RunResult.Completed -> run.copy(timings = timings)
            is RunResult.RuntimeError -> run.copy(timings = timings)
            is RunResult.LimitExceeded -> run.copy(timings = timings)
            else -> run
        }
    }

    @Synchronized
    private fun reply(replyTo: Messenger, result: RunResult) {
        if (replied) return
        replied = true
        try {
            replyTo.send(Message.obtain(null, RunnerProtocol.MSG_RESULT).apply { data = RunnerProtocol.encodeResult(result) })
        } catch (e: RemoteException) {
            Log.w(TAG, "La app ya no escucha", e)
        }
    }

    private var replied = false

    private fun terminate() {
        Process.killProcess(Process.myPid())
    }

    companion object {
        private const val TAG = "FuncodeRunner"
        const val BUSY = "busy"
        private const val WATCHDOG_MARGIN_MS = 2_000L
    }
}
