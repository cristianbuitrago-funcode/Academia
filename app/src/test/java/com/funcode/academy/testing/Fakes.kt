package com.funcode.academy.testing

import com.funcode.academy.domain.lesson.AssessmentScore
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.CourseModule
import com.funcode.academy.domain.lesson.Lesson
import com.funcode.academy.domain.lesson.LessonLoadResult
import com.funcode.academy.domain.lesson.LessonSummary
import com.funcode.academy.domain.lesson.ModuleLoadResult
import com.funcode.academy.domain.lesson.toSummary
import com.funcode.academy.domain.model.Audience
import com.funcode.academy.domain.progress.ExerciseAttempt
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.progress.XpAward
import com.funcode.academy.domain.progress.XpAwardResult
import com.funcode.academy.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Repositorios en memoria para probar la interfaz sin base de datos. */
class FakeProgressRepository(initialXp: Int = 0) : ProgressRepository {
    private val total = MutableStateFlow(initialXp)
    private val solved = MutableStateFlow(emptySet<String>())
    private val bestScores = MutableStateFlow(emptyMap<String, Double>())
    val attempts = mutableListOf<ExerciseAttempt>()

    /** Cada resultado de evaluación recibido, en orden (para comprobar que no se duplica). */
    val assessmentResults = mutableListOf<Pair<String, AssessmentScore>>()

    override fun observeTotalXp(): Flow<Int> = total
    override suspend fun award(award: XpAward): XpAwardResult = error("No se usa en las pruebas de interfaz")
    override suspend fun recordAttempt(attempt: ExerciseAttempt) {
        attempts += attempt
        if (attempt.correct) solved.update { it + attempt.exerciseId }
    }

    override fun observeSolvedExerciseIds(): Flow<Set<String>> = solved

    override suspend fun recordAssessmentResult(moduleId: String, score: AssessmentScore) {
        assessmentResults += moduleId to score
        bestScores.update { it + (moduleId to maxOf(score.fraction, it[moduleId] ?: 0.0)) }
    }

    override fun observeBestAssessmentScores(): Flow<Map<String, Double>> = bestScores
}

class FakeSettingsRepository(initial: Audience? = null) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val audience: Flow<Audience?> = state
    override suspend fun setAudience(audience: Audience) {
        state.value = audience
    }
}

class FakeContentRepository(
    private val lessons: List<Lesson> = emptyList(),
    private val modules: List<CourseModule> = emptyList(),
) : ContentRepository {
    override suspend fun listLessons(): List<LessonSummary> = lessons.map { it.toSummary() }
    override suspend fun listModules(): List<CourseModule> = modules
    override suspend fun loadModule(moduleId: String): ModuleLoadResult =
        modules.firstOrNull { it.id == moduleId }?.let { ModuleLoadResult.Loaded(it) } ?: ModuleLoadResult.NotFound
    override suspend fun loadLesson(lessonId: String): LessonLoadResult =
        lessons.firstOrNull { it.id == lessonId }?.let { LessonLoadResult.Loaded(it) } ?: LessonLoadResult.NotFound
}
