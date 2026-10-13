package com.funcode.academy.domain.lesson

/**
 * Estado de una sesión de ejercicios de una lección. Es inmutable: cada acción
 * devuelve una sesión nueva, lo que facilita probarla y restaurarla en la interfaz.
 *
 * Reglas:
 * - Solo las respuestas completas ([AnswerResult.Correct]/[AnswerResult.Incorrect]) cuentan como intentos.
 * - Un ejercicio resuelto no admite más intentos.
 * - Las pistas se revelan de una en una y quedan registradas.
 * - En una evaluación ([LessonKind.ASSESSMENT]) cada pregunta admite una sola
 *   respuesta completa y no hay pistas.
 */
data class LessonSession(
    val lesson: Lesson,
    val currentIndex: Int = 0,
    val progress: Map<String, ExerciseProgress> = emptyMap(),
    val finished: Boolean = false,
) {
    val currentExercise: Exercise get() = lesson.exercises[currentIndex]
    val currentProgress: ExerciseProgress get() = progressOf(currentExercise.id)
    val isLastExercise: Boolean get() = currentIndex == lesson.exercises.lastIndex

    /** En las lecciones y los retos se puede reintentar; en las evaluaciones, no. */
    val allowsRetries: Boolean get() = lesson.kind != LessonKind.ASSESSMENT

    fun progressOf(exerciseId: String): ExerciseProgress = progress[exerciseId] ?: ExerciseProgress()

    /** Revela la siguiente pista del ejercicio actual (si quedan). */
    fun revealHint(): LessonSession {
        val p = currentProgress
        if (!allowsRetries || p.hintsRevealed >= currentExercise.hints.size || p.solved) return this
        return withProgress(p.copy(hintsRevealed = p.hintsRevealed + 1))
    }

    /** Comprueba una respuesta del ejercicio actual. */
    fun submit(answer: Answer): Submission {
        val p = currentProgress
        check(!p.solved) { "El ejercicio ${currentExercise.id} ya está resuelto" }
        check(allowsRetries || p.attempts == 0) { "En una evaluación cada pregunta admite una sola respuesta" }
        val result = AnswerValidator.validate(currentExercise.content, answer)
        val updated = when (result) {
            is AnswerResult.Incomplete -> p.copy(lastResult = result)
            AnswerResult.Correct -> p.copy(attempts = p.attempts + 1, solved = true, lastResult = result)
            is AnswerResult.Incorrect -> p.copy(attempts = p.attempts + 1, lastResult = result)
        }
        return Submission(withProgress(updated), result)
    }

    /** Pasa al siguiente ejercicio (resuelto o no) o termina la sesión tras el último. */
    fun next(): LessonSession =
        if (isLastExercise) copy(finished = true) else copy(currentIndex = currentIndex + 1)

    fun summary(): SessionSummary {
        val results = lesson.exercises.map { e ->
            val p = progressOf(e.id)
            ExerciseOutcome(e.id, p.solved, p.attempts, p.hintsRevealed)
        }
        val score = lesson.assessment?.let { a ->
            AssessmentScore(
                correct = a.exerciseIds.count { id -> results.first { it.exerciseId == id }.solvedFirstTry },
                total = a.exerciseIds.size,
            )
        }
        // Solo se declara aprobado/no aprobado si el umbral está definido (P6).
        val passed = score?.let { s -> lesson.assessment?.passThreshold?.let { s.fraction >= it } }
        return SessionSummary(results, score, passed)
    }

    private fun withProgress(p: ExerciseProgress) = copy(progress = progress + (currentExercise.id to p))

    data class Submission(val session: LessonSession, val result: AnswerResult)
}

data class ExerciseProgress(
    val attempts: Int = 0,
    val hintsRevealed: Int = 0,
    val solved: Boolean = false,
    val lastResult: AnswerResult? = null,
)

data class ExerciseOutcome(val exerciseId: String, val solved: Boolean, val attempts: Int, val hintsUsed: Int) {
    val solvedFirstTry: Boolean get() = solved && attempts == 1
}

/** Resultado de una evaluación: respuestas comprobadas correctas al primer intento. */
data class AssessmentScore(val correct: Int, val total: Int) {
    init {
        require(total > 0 && correct in 0..total) { "Resultado no válido: $correct de $total" }
    }

    val fraction: Double get() = correct.toDouble() / total

    /** Porcentaje entero, redondeado hacia abajo para no exagerar el resultado. */
    val percent: Int get() = correct * 100 / total
}

/** Resumen de la sesión. Todo se calcula a partir de los resultados; no se guarda aparte. */
data class SessionSummary(
    val outcomes: List<ExerciseOutcome>,
    /** `null` si la lección no tiene evaluación. */
    val assessmentScore: AssessmentScore?,
    /** `null` si no hay evaluación o si su umbral de aprobación no está definido. */
    val assessmentPassed: Boolean?,
) {
    val total: Int get() = outcomes.size
    val solved: Int get() = outcomes.count { it.solved }
    val solvedFirstTry: Int get() = outcomes.count { it.solvedFirstTry }
    val unsolved: Int get() = total - solved
    val hintsUsed: Int get() = outcomes.sumOf { it.hintsUsed }
    val attempts: Int get() = outcomes.sumOf { it.attempts }
}

/** Fuente de contenido educativo (en la app: archivos JSON en `assets`). */
interface ContentRepository {
    suspend fun listLessons(): List<LessonSummary>
    suspend fun loadLesson(lessonId: String): LessonLoadResult

    /** Módulos válidos, ordenados por `order`. */
    suspend fun listModules(): List<CourseModule>
    suspend fun loadModule(moduleId: String): ModuleLoadResult
}

sealed interface LessonLoadResult {
    data class Loaded(val lesson: Lesson) : LessonLoadResult
    data object NotFound : LessonLoadResult
    data class Invalid(val errors: List<String>) : LessonLoadResult
}
