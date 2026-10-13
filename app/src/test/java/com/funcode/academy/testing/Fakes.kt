package com.funcode.academy.testing

import com.funcode.academy.domain.lesson.AssessmentScore
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.CourseModule
import com.funcode.academy.domain.lesson.Lesson
import com.funcode.academy.domain.lesson.LessonLoadResult
import com.funcode.academy.domain.lesson.LessonSummary
import com.funcode.academy.domain.lesson.ModuleLoadResult
import com.funcode.academy.domain.lesson.toSummary
import com.funcode.academy.domain.mission.MissionDefinition
import com.funcode.academy.domain.model.Audience
import com.funcode.academy.domain.progress.ExerciseAttempt
import com.funcode.academy.domain.progress.ExerciseHistory
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.progress.XpAward
import com.funcode.academy.domain.progress.XpAwardResult
import com.funcode.academy.domain.progress.XpEntry
import com.funcode.academy.domain.settings.LearningPreferences
import com.funcode.academy.domain.settings.SettingsRepository
import com.funcode.academy.domain.settings.TextScale
import com.funcode.academy.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Repositorios en memoria para probar la interfaz sin base de datos. El registro de XP
 * y los logros tienen las mismas garantías que Room: una sola entrada por actividad.
 */
class FakeProgressRepository : ProgressRepository {
    private val ledger = MutableStateFlow(emptyList<XpEntry>())
    private val solved = MutableStateFlow(emptySet<String>())
    private val bestScores = MutableStateFlow(emptyMap<String, Double>())
    private val achievements = MutableStateFlow(emptyMap<String, Long>())
    val attempts = mutableListOf<ExerciseAttempt>()

    /** Cada resultado de evaluación recibido, en orden (para comprobar que no se duplica). */
    val assessmentResults = mutableListOf<Pair<String, AssessmentScore>>()

    val ledgerEntries: List<XpEntry> get() = ledger.value
    val unlockedAchievements: Set<String> get() = achievements.value.keys

    override fun observeTotalXp(): Flow<Int> = ledger.map { l -> l.sumOf { it.amount } }

    override suspend fun award(award: XpAward): XpAwardResult {
        if (ledger.value.any { it.source == award.source }) return XpAwardResult.AlreadyGranted
        ledger.update { listOf(XpEntry(award.source, award.amount, timestamp = 0)) + it }
        return XpAwardResult.Granted(award.amount)
    }

    override suspend fun recordAttempt(attempt: ExerciseAttempt) {
        attempts += attempt
        attemptsFlow.value = attempts.toList()
        if (attempt.correct) solved.update { it + attempt.exerciseId }
    }

    override fun observeSolvedExerciseIds(): Flow<Set<String>> = solved

    override suspend fun recordAssessmentResult(moduleId: String, score: AssessmentScore) {
        assessmentResults += moduleId to score
        bestScores.update { it + (moduleId to maxOf(score.fraction, it[moduleId] ?: 0.0)) }
    }

    override fun observeBestAssessmentScores(): Flow<Map<String, Double>> = bestScores

    override suspend fun firstCorrectAttempts(): Map<String, Int> =
        attempts.filter { it.correct }.groupBy { it.exerciseId }.mapValues { it.value.first().hintsUsed }

    override fun observeXpEntries(): Flow<List<XpEntry>> = ledger

    override suspend fun unlockAchievement(achievementId: String): Boolean {
        if (achievementId in achievements.value) return false
        achievements.update { it + (achievementId to 0L) }
        return true
    }

    override fun observeUnlockedAchievements(): Flow<Map<String, Long>> = achievements

    private val attemptsFlow = MutableStateFlow(emptyList<ExerciseAttempt>())

    override fun observeExerciseHistories(): Flow<Map<String, ExerciseHistory>> =
        attemptsFlow.map { ExerciseHistory.fromAttempts(it) }
}

class FakeSettingsRepository(initial: LearningPreferences = LearningPreferences()) : SettingsRepository {
    constructor(initial: Audience?) : this(LearningPreferences(audience = initial))

    private val state = MutableStateFlow(initial)
    override val preferences: Flow<LearningPreferences> = state
    override val audience: Flow<Audience?> = state.map { it.audience }
    val current: LearningPreferences get() = state.value

    override suspend fun setAudience(audience: Audience) = state.update { it.copy(audience = audience) }
    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override suspend fun setTextScale(scale: TextScale) = state.update { it.copy(textScale = scale) }
    override suspend fun setHintsEnabled(enabled: Boolean) = state.update { it.copy(hintsEnabled = enabled) }
    override suspend fun resetPreferences() {
        state.value = LearningPreferences()
    }
}

class FakeContentRepository(
    private val lessons: List<Lesson> = emptyList(),
    private val modules: List<CourseModule> = emptyList(),
    private val missions: List<MissionDefinition> = emptyList(),
) : ContentRepository {
    override suspend fun listMissions(): List<MissionDefinition> = missions
    override suspend fun listLessons(): List<LessonSummary> = lessons.map { it.toSummary() }
    override suspend fun listModules(): List<CourseModule> = modules
    override suspend fun loadModule(moduleId: String): ModuleLoadResult =
        modules.firstOrNull { it.id == moduleId }?.let { ModuleLoadResult.Loaded(it) } ?: ModuleLoadResult.NotFound
    override suspend fun loadLesson(lessonId: String): LessonLoadResult =
        lessons.firstOrNull { it.id == lessonId }?.let { LessonLoadResult.Loaded(it) } ?: LessonLoadResult.NotFound
}
