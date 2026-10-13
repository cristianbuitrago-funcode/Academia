package com.funcode.academy.domain.progress

/**
 * Resumen de los intentos guardados de un ejercicio, para las recomendaciones de
 * repaso. Se deduce de `exercise_attempt`; no se guarda aparte.
 */
data class ExerciseHistory(
    val exerciseId: String,
    val attempts: Int,
    /** Hubo al menos un intento incorrecto antes del primer acierto. */
    val failedBeforeFirstCorrect: Boolean,
    /** Pistas usadas en el primer acierto, o `null` si aún no se ha resuelto. */
    val hintsAtFirstCorrect: Int?,
) {
    val solved: Boolean get() = hintsAtFirstCorrect != null

    /** Le costó: lo resolvió tras fallar o con pistas. */
    val struggled: Boolean get() = solved && (failedBeforeFirstCorrect || hintsAtFirstCorrect!! > 0)

    companion object {
        /** Construye el historial de cada ejercicio a partir de sus intentos, en el orden en que se hicieron. */
        fun fromAttempts(attempts: List<ExerciseAttempt>): Map<String, ExerciseHistory> =
            attempts.groupBy { it.exerciseId }.mapValues { (id, list) ->
                val firstCorrect = list.indexOfFirst { it.correct }
                ExerciseHistory(
                    exerciseId = id,
                    attempts = list.size,
                    failedBeforeFirstCorrect = if (firstCorrect < 0) list.isNotEmpty() else firstCorrect > 0,
                    hintsAtFirstCorrect = if (firstCorrect < 0) null else list[firstCorrect].hintsUsed,
                )
            }
    }
}
