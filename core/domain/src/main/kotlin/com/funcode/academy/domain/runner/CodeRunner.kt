package com.funcode.academy.domain.runner

/**
 * Contrato para compilar y ejecutar el programa de un estudiante (Fase 8, prototipo).
 *
 * El código del estudiante se trata siempre como **no confiable**. Las implementaciones
 * deben ejecutar fuera del proceso de la interfaz y aplicar [RunLimits]. Un resultado solo
 * puede decir que el programa se ejecutó si de verdad se ejecutó ([RunResult.Completed] o
 * [RunResult.RuntimeError]).
 */
interface CodeRunner {
    /** Qué puede hacer este ejecutor; la interfaz decide con esto si ofrece «Compilar y ejecutar». */
    val capabilities: RunnerCapabilities

    /** Compila y ejecuta. No lanza excepciones por el código del estudiante: todo va en el resultado. */
    suspend fun run(request: RunRequest): RunResult
}

data class RunnerCapabilities(
    val canExecute: Boolean,
    /** Entrada estándar (Scanner): aplazada por la decisión F8-D7. */
    val supportsStdin: Boolean = false,
    /** Nivel de lenguaje (F8-D5). */
    val javaLanguageLevel: Int = 17,
)

/** El prototipo admite un único archivo `Main.java` con una clase `Main` y su método `main`. */
data class RunRequest(
    val source: String,
    val limits: RunLimits = RunLimits(),
)

/** Límites aprobados (F8-D4). */
data class RunLimits(
    val timeoutMs: Long = 3_000,
    val maxSourceChars: Int = 10_000,
    val maxOutputChars: Int = 10_000,
    /** Tiempo máximo para compilar y preparar el programa (no cuenta en el límite del estudiante). */
    val compileTimeoutMs: Long = 15_000,
) {
    init {
        require(timeoutMs > 0 && maxSourceChars > 0 && maxOutputChars > 0 && compileTimeoutMs > 0)
    }
}

/** Mensaje de compilación con su posición (línea 1 = primera línea del código). */
data class Diagnostic(val line: Int?, val message: String)

/** Tiempos medidos, para documentar el rendimiento real (no se muestran como logro). */
data class RunTimings(val compileMs: Long? = null, val dexMs: Long? = null, val executeMs: Long? = null, val totalMs: Long? = null)

sealed interface RunResult {
    val timings: RunTimings

    /** El código no se envió a compilar (vacío o demasiado largo). No hubo compilación ni ejecución. */
    data class Rejected(val reason: RejectReason, override val timings: RunTimings = RunTimings()) : RunResult

    /** El compilador real informó errores. No hubo ejecución. */
    data class CompilationFailed(val diagnostics: List<Diagnostic>, override val timings: RunTimings = RunTimings()) : RunResult

    /** El programa se ejecutó y terminó con normalidad. */
    data class Completed(val stdout: String, override val timings: RunTimings = RunTimings()) : RunResult

    /** El programa se ejecutó y terminó por una excepción (p. ej. división entre cero). */
    data class RuntimeError(
        val exception: String,
        val message: String?,
        val line: Int?,
        val stdout: String,
        override val timings: RunTimings = RunTimings(),
    ) : RunResult

    /** Se alcanzó un límite y el programa se detuvo. */
    data class LimitExceeded(val limit: Limit, val stdout: String, override val timings: RunTimings = RunTimings()) : RunResult

    /** Lo detuvo el estudiante. */
    data class Cancelled(override val timings: RunTimings = RunTimings()) : RunResult

    /** No se pudo compilar o ejecutar por un problema del ejecutor (no del estudiante). */
    data class Unavailable(val reason: String, override val timings: RunTimings = RunTimings()) : RunResult
}

enum class RejectReason { EMPTY_SOURCE, SOURCE_TOO_LARGE }

enum class Limit {
    /** Más tiempo que [RunLimits.timeoutMs]. */
    TIME,
    /** Más salida que [RunLimits.maxOutputChars]. */
    OUTPUT,
    /** Memoria agotada o el proceso de ejecución terminó de forma inesperada. */
    MEMORY,
}
