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
import com.funcode.academy.domain.lesson.LessonKind
import com.funcode.academy.domain.lesson.LessonLoadResult
import com.funcode.academy.domain.lesson.LessonSession
import com.funcode.academy.domain.lesson.ModuleLoadResult
import com.funcode.academy.domain.lesson.Section
import com.funcode.academy.domain.lesson.SessionSummary
import com.funcode.academy.domain.progress.ExerciseAttempt
import com.funcode.academy.domain.progress.ModuleProgress
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.progress.XpSourceType
import com.funcode.academy.domain.rpg.Achievement
import com.funcode.academy.domain.rpg.RewardEngine
import com.funcode.academy.domain.rpg.RewardOutcome
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

    /** Reto aún cerrado: faltan [lessonsRemaining] lecciones del módulo por completar. */
    data class Locked(val lesson: Lesson, val lessonsRemaining: Int) : LessonUiState

    data class Exercising(
        val session: LessonSession,
        /** Respuesta que el estudiante está preparando (aún no comprobada). */
        val draft: Answer,
        /** Resultado de la última comprobación; `null` mientras edita. */
        val feedback: AnswerResult?,
    ) : LessonUiState

    data class Finished(val lesson: Lesson, val summary: SessionSummary) : LessonUiState
}

/** Aviso de recompensas obtenidas tras una respuesta comprobada o al terminar una evaluación. */
data class RewardNotice(
    val xpGained: Int,
    val lessonCompleted: Boolean,
    val assessmentPassed: Boolean,
    val challengeCompleted: Boolean,
    /** Nuevo nivel alcanzado, o `null` si no se ha subido de nivel. */
    val newLevel: Int?,
    val achievements: List<Achievement>,
    /** El ejercicio ya se había recompensado antes: practicar no da XP. */
    val alreadyRewarded: Boolean,
) {
    companion object {
        fun from(outcome: RewardOutcome, exerciseId: String?): RewardNotice? {
            val types = outcome.granted.map { it.source.type }
            val notice = RewardNotice(
                xpGained = outcome.xpGained,
                lessonCompleted = XpSourceType.LESSON in types,
                assessmentPassed = XpSourceType.ASSESSMENT in types,
                challengeCompleted = XpSourceType.CHALLENGE in types,
                newLevel = outcome.levelAfter.takeIf { outcome.leveledUp },
                achievements = outcome.newAchievements,
                alreadyRewarded = exerciseId != null &&
                    outcome.granted.none { it.source.type == XpSourceType.EXERCISE && it.source.id == exerciseId },
            )
            return notice.takeIf { it.xpGained > 0 || it.alreadyRewarded || it.newLevel != null || it.achievements.isNotEmpty() }
        }
    }
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
    private val rewardEngine: RewardEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LessonUiState>(LessonUiState.Loading)
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    private val _rewardNotice = MutableStateFlow<RewardNotice?>(null)

    /** Última recompensa obtenida (se oculta al pasar al siguiente ejercicio). */
    val rewardNotice: StateFlow<RewardNotice?> = _rewardNotice.asStateFlow()

    private val _sessionXp = MutableStateFlow(0)

    /** XP conseguido en esta sesión de ejercicios. */
    val sessionXp: StateFlow<Int> = _sessionXp.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = when (val result = contentRepository.loadLesson(lessonId)) {
                is LessonLoadResult.Loaded -> {
                    val lesson = result.lesson
                    val remaining = if (lesson.kind == LessonKind.CHALLENGE) lessonsRemainingFor(lesson) else 0
                    if (remaining > 0) {
                        LessonUiState.Locked(lesson, remaining)
                    } else {
                        val audience = settingsRepository.audience.first()
                        LessonUiState.Intro(lesson, lesson.sectionsFor(audience))
                    }
                }
                LessonLoadResult.NotFound -> LessonUiState.NotFound
                is LessonLoadResult.Invalid -> LessonUiState.InvalidContent
            }
        }
    }

    /**
     * Lecciones que faltan para abrir un reto, según el progreso guardado. Se comprueba
     * aquí además de en la pantalla del módulo, para que no se pueda abrir de otra forma.
     */
    private suspend fun lessonsRemainingFor(challenge: Lesson): Int {
        val module = (contentRepository.loadModule(challenge.moduleId) as? ModuleLoadResult.Loaded)?.module ?: return 0
        val progress = ModuleProgress.of(
            module,
            progressRepository.observeSolvedExerciseIds().first(),
            progressRepository.observeBestAssessmentScores().first(),
        )
        return progress.challenge?.lessonsRemaining ?: 0
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
        _rewardNotice.value = null
        _sessionXp.value = 0
    }

    /** Cambia la respuesta en preparación; no se puede tocar una pregunta ya cerrada. */
    fun updateDraft(answer: Answer) = updateExercising { if (it.session.isCurrentClosed) it else it.copy(draft = answer, feedback = null) }

    fun revealHint() = updateExercising { it.copy(session = it.session.revealHint()) }

    fun check() {
        val state = _uiState.value as? LessonUiState.Exercising ?: return
        if (state.session.isCurrentClosed) return
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
                val saved = runCatching { progressRepository.recordAttempt(attempt) }.isSuccess
                // Las recompensas se calculan a partir de lo guardado; en una evaluación,
                // solo al terminarla.
                if (saved && attempt.correct && state.session.allowsRetries) {
                    // Los pasos de un reto no dan XP por separado: no se avisa de "ya recompensado".
                    syncRewards(exercise.id.takeIf { state.session.lesson.kind == LessonKind.LESSON })
                }
            }
        }
    }

    /** Oculta la retroalimentación para volver a intentarlo, conservando la respuesta. */
    fun retry() = updateExercising { if (it.session.isCurrentClosed) it else it.copy(feedback = null) }

    /** Siguiente ejercicio, o resumen tras el último. */
    fun next() {
        val state = _uiState.value as? LessonUiState.Exercising ?: return
        val session = state.session.next()
        if (!session.finished) {
            _uiState.value = LessonUiState.Exercising(session, initialDraft(session.currentExercise), feedback = null)
            _rewardNotice.value = null
            return
        }
        val summary = session.summary()
        _uiState.value = LessonUiState.Finished(session.lesson, summary)
        // El resultado de una evaluación se guarda una vez por sesión terminada; el
        // repositorio conserva solo el mejor, así que repetirla nunca lo duplica.
        val score = summary.assessmentScore
        if (session.lesson.kind == LessonKind.ASSESSMENT && score != null) {
            viewModelScope.launch {
                val saved = runCatching { progressRepository.recordAssessmentResult(session.lesson.moduleId, score) }.isSuccess
                if (saved) syncRewards(exerciseId = null)
            }
        }
    }

    /**
     * Pide al motor que conceda lo que corresponda según lo guardado. Si falla, no se
     * pierde nada: la siguiente sincronización (p. ej. al abrir Inicio) lo concede.
     */
    private suspend fun syncRewards(exerciseId: String?) {
        val outcome = runCatching { rewardEngine.sync() }.getOrNull() ?: return
        _sessionXp.update { it + outcome.xpGained }
        RewardNotice.from(outcome, exerciseId)?.let { _rewardNotice.value = it }
    }

    /** Resuelta, o ya respondida en una evaluación (una sola respuesta por pregunta). */
    private val LessonSession.isCurrentClosed: Boolean
        get() = currentProgress.solved || (!allowsRetries && currentProgress.attempts > 0)

    private fun updateExercising(transform: (LessonUiState.Exercising) -> LessonUiState.Exercising) =
        _uiState.update { if (it is LessonUiState.Exercising) transform(it) else it }

    companion object {
        fun factory(
            lessonId: String,
            contentRepository: ContentRepository,
            progressRepository: ProgressRepository,
            settingsRepository: SettingsRepository,
            rewardEngine: RewardEngine,
        ) = viewModelFactory {
            initializer { LessonViewModel(lessonId, contentRepository, progressRepository, settingsRepository, rewardEngine) }
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
