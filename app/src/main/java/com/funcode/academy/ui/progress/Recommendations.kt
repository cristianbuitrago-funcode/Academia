package com.funcode.academy.ui.progress

import com.funcode.academy.domain.learning.Recommendation
import com.funcode.academy.domain.learning.StudyAdvisor
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.LessonLoadResult
import com.funcode.academy.domain.progress.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

/**
 * Recomendaciones de estudio, recalculadas con cada intento o resultado guardado.
 * Reglas sencillas de [StudyAdvisor]; no hay inteligencia artificial.
 */
fun observeRecommendations(content: ContentRepository, progress: ProgressRepository): Flow<List<Recommendation>> {
    val modulesAndThresholds = flow {
        val modules = content.listModules()
        val thresholds = modules.mapNotNull { it.assessment }.mapNotNull { summary ->
            (content.loadLesson(summary.id) as? LessonLoadResult.Loaded)?.lesson?.assessment?.passThreshold?.let { summary.id to it }
        }.toMap()
        emit(modules to thresholds)
    }
    return combine(modulesAndThresholds, progress.observeExerciseHistories(), progress.observeBestAssessmentScores()) {
            (modules, thresholds), histories, scores ->
        StudyAdvisor.recommend(modules, histories, scores, thresholds)
    }
}
