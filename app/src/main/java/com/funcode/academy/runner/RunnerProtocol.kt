package com.funcode.academy.runner

import android.os.Bundle
import com.funcode.academy.domain.runner.Diagnostic
import com.funcode.academy.domain.runner.Limit
import com.funcode.academy.domain.runner.RejectReason
import com.funcode.academy.domain.runner.RunLimits
import com.funcode.academy.domain.runner.RunRequest
import com.funcode.academy.domain.runner.RunResult
import com.funcode.academy.domain.runner.RunTimings

/**
 * Mensajes entre la app y el proceso aislado ([JavaRunnerService]). Solo tipos simples
 * (texto y números) en un [Bundle]: el proceso aislado no recibe ni devuelve objetos.
 */
internal object RunnerProtocol {
    const val MSG_RUN = 1
    const val MSG_CANCEL = 2
    const val MSG_RESULT = 3

    // Solicitud
    private const val SOURCE = "source"
    private const val TIMEOUT = "timeoutMs"
    private const val MAX_SOURCE = "maxSourceChars"
    private const val MAX_OUTPUT = "maxOutputChars"
    private const val COMPILE_TIMEOUT = "compileTimeoutMs"

    /**
     * SOLO PRUEBAS EN COMPILACIONES DE DEPURACIÓN (el servicio lo ignora en las demás):
     * declaraciones extra para el compilador y desactivar la lista permitida en ejecución.
     * Sirven para comprobar por separado cada capa de aislamiento.
     */
    const val DIAG_EXTRA_LIBRARY = "diag.extraLibrary"
    const val DIAG_UNRESTRICTED_LOADER = "diag.unrestrictedLoader"

    // Resultado
    private const val TYPE = "type"
    private const val TEXT = "text"
    private const val STDOUT = "stdout"
    private const val LINE = "line"
    private const val LINES = "lines"
    private const val MESSAGES = "messages"
    private const val COMPILE_MS = "compileMs"
    private const val DEX_MS = "dexMs"
    private const val EXECUTE_MS = "executeMs"
    private const val TOTAL_MS = "totalMs"

    fun encodeRequest(request: RunRequest, diagnostics: Bundle? = null) = Bundle().apply {
        putString(SOURCE, request.source)
        putLong(TIMEOUT, request.limits.timeoutMs)
        putInt(MAX_SOURCE, request.limits.maxSourceChars)
        putInt(MAX_OUTPUT, request.limits.maxOutputChars)
        putLong(COMPILE_TIMEOUT, request.limits.compileTimeoutMs)
        diagnostics?.let { putAll(it) }
    }

    fun decodeRequest(b: Bundle) = RunRequest(
        source = b.getString(SOURCE).orEmpty(),
        limits = RunLimits(
            timeoutMs = b.getLong(TIMEOUT),
            maxSourceChars = b.getInt(MAX_SOURCE),
            maxOutputChars = b.getInt(MAX_OUTPUT),
            compileTimeoutMs = b.getLong(COMPILE_TIMEOUT),
        ),
    )

    fun encodeResult(r: RunResult) = Bundle().apply {
        putLong(COMPILE_MS, r.timings.compileMs ?: -1)
        putLong(DEX_MS, r.timings.dexMs ?: -1)
        putLong(EXECUTE_MS, r.timings.executeMs ?: -1)
        putLong(TOTAL_MS, r.timings.totalMs ?: -1)
        when (r) {
            is RunResult.Rejected -> { putString(TYPE, "rejected"); putString(TEXT, r.reason.name) }
            is RunResult.CompilationFailed -> {
                putString(TYPE, "compile")
                putIntArray(LINES, r.diagnostics.map { it.line ?: -1 }.toIntArray())
                putStringArray(MESSAGES, r.diagnostics.map { it.message }.toTypedArray())
            }
            is RunResult.Completed -> { putString(TYPE, "completed"); putString(STDOUT, r.stdout) }
            is RunResult.RuntimeError -> {
                putString(TYPE, "runtime"); putString(TEXT, r.exception); putString(MESSAGES, r.message)
                putInt(LINE, r.line ?: -1); putString(STDOUT, r.stdout)
            }
            is RunResult.LimitExceeded -> { putString(TYPE, "limit"); putString(TEXT, r.limit.name); putString(STDOUT, r.stdout) }
            is RunResult.Cancelled -> putString(TYPE, "cancelled")
            is RunResult.Unavailable -> { putString(TYPE, "unavailable"); putString(TEXT, r.reason) }
        }
    }

    fun decodeResult(b: Bundle): RunResult {
        fun ms(key: String) = b.getLong(key, -1).takeIf { it >= 0 }
        val t = RunTimings(ms(COMPILE_MS), ms(DEX_MS), ms(EXECUTE_MS), ms(TOTAL_MS))
        val stdout = b.getString(STDOUT).orEmpty()
        return when (b.getString(TYPE)) {
            "rejected" -> RunResult.Rejected(RejectReason.valueOf(b.getString(TEXT)!!), t)
            "compile" -> {
                val lines = b.getIntArray(LINES) ?: IntArray(0)
                val messages = b.getStringArray(MESSAGES) ?: emptyArray()
                RunResult.CompilationFailed(messages.mapIndexed { i, m -> Diagnostic(lines.getOrNull(i)?.takeIf { it > 0 }, m) }, t)
            }
            "completed" -> RunResult.Completed(stdout, t)
            "runtime" -> RunResult.RuntimeError(b.getString(TEXT).orEmpty(), b.getString(MESSAGES), b.getInt(LINE, -1).takeIf { it > 0 }, stdout, t)
            "limit" -> RunResult.LimitExceeded(Limit.valueOf(b.getString(TEXT)!!), stdout, t)
            "cancelled" -> RunResult.Cancelled(t)
            else -> RunResult.Unavailable(b.getString(TEXT) ?: "Respuesta desconocida del ejecutor", t)
        }
    }
}
