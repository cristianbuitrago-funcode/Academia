package com.funcode.academy.domain.runner

import com.funcode.academy.domain.lesson.CodeText

/**
 * Evalúa el resultado de una ejecución real frente a las salidas aceptadas de un ejercicio.
 *
 * Reglas (decisión F8-D6):
 * - Ejecutar no da XP por sí mismo: solo se registra un intento, y el XP lo decide el
 *   motor de recompensas existente (primer acierto, una sola vez).
 * - El intento se registra cuando la ejecución **terminó**: con normalidad (correcto o no
 *   según la salida) o por una excepción del programa (incorrecto).
 * - Errores de compilación, código rechazado, límites, cancelación o ejecutor no disponible
 *   **no** registran intento: no penalizan ni premian.
 * - Una salida real no es automáticamente correcta: debe coincidir con una aceptada.
 */
object ProgramEvaluation {

    sealed interface Outcome {
        /** `null` = no se registra intento; `true`/`false` = intento correcto/incorrecto. */
        val attemptCorrect: Boolean?

        data object OutputMatches : Outcome { override val attemptCorrect = true }
        data object OutputDiffers : Outcome { override val attemptCorrect = false }
        data object ProgramFailed : Outcome { override val attemptCorrect = false }
        data object NotExecuted : Outcome { override val attemptCorrect: Boolean? = null }
    }

    fun evaluate(result: RunResult, acceptedOutputs: List<String>): Outcome {
        require(acceptedOutputs.isNotEmpty()) { "Un ejercicio de programa necesita al menos una salida aceptada" }
        return when (result) {
            is RunResult.Completed -> {
                val given = CodeText.normalizeOutput(result.stdout)
                if (acceptedOutputs.any { CodeText.normalizeOutput(it) == given }) Outcome.OutputMatches else Outcome.OutputDiffers
            }
            is RunResult.RuntimeError -> Outcome.ProgramFailed
            is RunResult.Rejected, is RunResult.CompilationFailed, is RunResult.LimitExceeded,
            is RunResult.Cancelled, is RunResult.Unavailable -> Outcome.NotExecuted
        }
    }

    /** Validación previa común a todos los ejecutores (no sustituye a los límites de ejecución). */
    fun precheck(request: RunRequest): RunResult.Rejected? = when {
        request.source.isBlank() -> RunResult.Rejected(RejectReason.EMPTY_SOURCE)
        request.source.length > request.limits.maxSourceChars -> RunResult.Rejected(RejectReason.SOURCE_TOO_LARGE)
        else -> null
    }
}
