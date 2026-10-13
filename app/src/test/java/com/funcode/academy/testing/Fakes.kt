package com.funcode.academy.testing

import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.Lesson
import com.funcode.academy.domain.lesson.LessonLoadResult
import com.funcode.academy.domain.lesson.LessonSummary
import com.funcode.academy.domain.lesson.toSummary
import com.funcode.academy.domain.model.Audience
import com.funcode.academy.domain.progress.ExerciseAttempt
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.progress.XpAward
import com.funcode.academy.domain.progress.XpAwardResult
import com.funcode.academy.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Repositorios en memoria para probar la interfaz sin base de datos. */
class FakeProgressRepository(initialXp: Int = 0) : ProgressRepository {
    private val total = MutableStateFlow(initialXp)
    val attempts = mutableListOf<ExerciseAttempt>()
    override fun observeTotalXp(): Flow<Int> = total
    override suspend fun award(award: XpAward): XpAwardResult = error("No se usa en las pruebas de interfaz")
    override suspend fun recordAttempt(attempt: ExerciseAttempt) {
        attempts += attempt
    }
}

class FakeSettingsRepository(initial: Audience? = null) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val audience: Flow<Audience?> = state
    override suspend fun setAudience(audience: Audience) {
        state.value = audience
    }
}

class FakeContentRepository(private val lessons: List<Lesson> = emptyList()) : ContentRepository {
    override suspend fun listLessons(): List<LessonSummary> = lessons.map { it.toSummary() }
    override suspend fun loadLesson(lessonId: String): LessonLoadResult =
        lessons.firstOrNull { it.id == lessonId }?.let { LessonLoadResult.Loaded(it) } ?: LessonLoadResult.NotFound
}
