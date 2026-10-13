package com.funcode.academy.ui.screens.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funcode.academy.domain.lesson.Answer
import com.funcode.academy.domain.lesson.AnswerResult
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.Exercise
import com.funcode.academy.domain.lesson.ExerciseContent
import com.funcode.academy.domain.lesson.Lesson
import com.funcode.academy.domain.lesson.LessonLoadResult
import com.funcode.academy.domain.lesson.LessonSession
import com.funcode.academy.domain.lesson.Section
import com.funcode.academy.domain.lesson.SessionSummary
import com.funcode.academy.domain.progress.ExerciseAttempt
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Estado de la pantalla de lección. */
sealed interface LessonUiState {
    data object Loading : LessonUiState
    data object NotFound : LessonUiState

    /** El contenido existe pero tiene errores; se muestra un aviso en lugar de cerrar la app. */
    data object InvalidContent : LessonUiState

    data class Intro(val lesson: Lesson, val sections: List<Section>) : LessonUiState

    data class Exercising(
        val session: LessonSession,
        /** Respuesta que el estudiante está preparando (aún no comprobada). */
        val draft: Answer,
        /** Resultado de la última comprobación; `null` mientras edita. */
        val feedback: AnswerResult?,
    ) : LessonUiState

    data class Finished(val lesson: Lesson, val summary: SessionSummary) : LessonUiState
}

/**
 * Coordina una lección: carga el contenido, mantiene la sesión del dominio
 * ([LessonSession]) y registra los intentos comprobados. No valida respuestas por
 * su cuenta: eso lo hace el dominio.
 */
class LessonViewModel(
    private val lessonId: String,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LessonUiState>(LessonUiState.Loading)
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = when (val result = contentRepository.loadLesson(lessonId)) {
                is LessonLoadResult.Loaded -> {
                    val audience = settingsRepository.audience.first()
                    LessonUiState.Intro(result.lesson, result.lesson.sectionsFor(audience))
                }
                LessonLoadResult.NotFound -> LessonUiState.NotFound
                is LessonLoadResult.Invalid -> LessonUiState.InvalidContent
            }
        }
    }

    /** Empieza (o repite) los ejercicios desde el primero. */
    fun startExercises() {
        val lesson = when (val s = _uiState.value) {
            is LessonUiState.Intro -> s.lesson
            is LessonUiState.Finished -> s.lesson
            else -> return
        }
        val session = LessonSession(lesson)
        _uiState.value = LessonUiState.Exercising(session, initialDraft(session.currentExercise), feedback = null)
    }

    fun updateDraft(answer: Answer) = updateExercising { it.copy(draft = answer, feedback = null) }

    fun revealHint() = updateExercising { it.copy(session = it.session.revealHint()) }

    fun check() {
        val state = _uiState.value as? LessonUiState.Exercising ?: return
        if (state.session.currentProgress.solved) return
        val submission = state.session.submit(state.draft)
        _uiState.value = state.copy(session = submission.session, feedback = submission.result)
        if (submission.result !is AnswerResult.Incomplete) {
            val exercise = state.session.currentExercise
            val attempt = ExerciseAttempt(
                exerciseId = exercise.id,
                correct = submission.result == AnswerResult.Correct,
                hintsUsed = submission.session.currentProgress.hintsRevealed,
            )
            viewModelScope.launch {
                // Un fallo al guardar no debe interrumpir la lección; el intento ya está en la sesión.
                runCatching { progressRepository.recordAttempt(attempt) }
            }
        }
    }

    /** Oculta la retroalimentación para volver a intentarlo, conservando la respuesta. */
    fun retry() = updateExercising { it.copy(feedback = null) }

    /** Siguiente ejercicio, o resumen tras el último. */
    fun next() {
        val state = _uiState.value as? LessonUiState.Exercising ?: return
        val session = state.session.next()
        _uiState.value = if (session.finished) {
            LessonUiState.Finished(session.lesson, session.summary())
        } else {
            LessonUiState.Exercising(session, initialDraft(session.currentExercise), feedback = null)
        }
    }

    private fun updateExercising(transform: (LessonUiState.Exercising) -> LessonUiState.Exercising) =
        _uiState.update { if (it is LessonUiState.Exercising) transform(it) else it }

    companion object {
        fun factory(
            lessonId: String,
            contentRepository: ContentRepository,
            progressRepository: ProgressRepository,
            settingsRepository: SettingsRepository,
        ) = viewModelFactory {
            initializer { LessonViewModel(lessonId, contentRepository, progressRepository, settingsRepository) }
        }

        /** Respuesta vacía inicial de cada tipo de ejercicio. */
        fun initialDraft(exercise: Exercise): Answer = when (val c = exercise.content) {
            is ExerciseContent.MultipleChoice -> Answer.Choice(emptySet())
            is ExerciseContent.TrueFalse -> Answer.TrueFalse(null)
            is ExerciseContent.FillInBlanks -> Answer.Blanks(List(c.acceptedAnswers.size) { "" })
            is ExerciseContent.OrderLines -> Answer.Order(shuffledOrder(exercise.id, c.lines.size))
            is ExerciseContent.PredictOutput -> Answer.Output("")
            is ExerciseContent.FindError -> Answer.ErrorLines(emptySet())
            is ExerciseContent.MatchPairs -> Answer.Pairs(emptyMap())
        }

        /**
         * Orden inicial desordenado y reproducible (depende solo del id del ejercicio),
         * nunca igual al orden correcto.
         */
        fun shuffledOrder(seed: String, size: Int): List<Int> {
            val identity = List(size) { it }
            val shuffled = identity.shuffled(Random(seed.hashCode()))
            return if (shuffled == identity && size > 1) identity.drop(1) + identity.first() else shuffled
        }
    }
}
