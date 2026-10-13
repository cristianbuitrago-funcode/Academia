package com.funcode.academy.domain.learning

import com.funcode.academy.domain.lesson.CourseModule
import com.funcode.academy.domain.lesson.LessonSummary
import com.funcode.academy.domain.progress.ExerciseHistory
import kotlin.math.roundToInt

/** Recomendación de estudio basada en resultados guardados. Nunca bloquea nada ni da XP. */
sealed interface Recommendation {
    /** Lección empezada (algún intento) pero sin completar. */
    data class ResumeLesson(val lesson: LessonSummary, val solved: Int, val total: Int) : Recommendation

    /** Lección completada en la que varios ejercicios costaron (fallos previos o pistas). */
    data class ReviewLesson(val lesson: LessonSummary, val struggledExercises: Int) : Recommendation

    /** Evaluación intentada sin alcanzar el umbral; se sugiere repasar antes de repetirla. */
    data class RetryAssessment(
        val assessment: LessonSummary,
        val bestPercent: Int,
        val thresholdPercent: Int,
        val lessonsToReview: List<LessonSummary>,
    ) : Recommendation
}

/**
 * Reglas sencillas y explicables (Fase 7, aprobadas por el responsable). No es un
 * sistema de inteligencia artificial: solo cuenta resultados reales.
 *
 * 1. Retomar: lección con al menos un intento guardado y sin completar.
 * 2. Repasar: lección completada con [STRUGGLED_TO_REVIEW] o más ejercicios que
 *    costaron (primer acierto tras un fallo o con pistas).
 * 3. Repetir la evaluación: mejor resultado por debajo del umbral; se acompaña de
 *    las lecciones de la regla 2 de ese módulo.
 */
object StudyAdvisor {
    const val STRUGGLED_TO_REVIEW = 2

    fun recommend(
        modules: List<CourseModule>,
        histories: Map<String, ExerciseHistory>,
        bestScores: Map<String, Double>,
        /** Umbral de aprobación de cada evaluación (id → fracción); sin umbral no se recomienda repetirla. */
        thresholds: Map<String, Double>,
    ): List<Recommendation> {
        val resume = mutableListOf<Recommendation>()
        val review = mutableListOf<Recommendation.ReviewLesson>()
        val retry = mutableListOf<Recommendation>()
        modules.forEach { module ->
            val moduleReview = mutableListOf<Recommendation.ReviewLesson>()
            module.lessons.forEach { lesson ->
                val h = lesson.exerciseIds.mapNotNull { histories[it] }
                val solved = h.count { it.solved }
                val total = lesson.exerciseIds.size
                when {
                    solved < total && h.isNotEmpty() -> resume += Recommendation.ResumeLesson(lesson, solved, total)
                    solved == total && total > 0 -> {
                        val struggled = h.count { it.struggled }
                        if (struggled >= STRUGGLED_TO_REVIEW) moduleReview += Recommendation.ReviewLesson(lesson, struggled)
                    }
                }
            }
            review += moduleReview
            val assessment = module.assessment
            val best = bestScores[module.id]
            val threshold = assessment?.let { thresholds[it.id] }
            if (assessment != null && best != null && threshold != null && best < threshold) {
                retry += Recommendation.RetryAssessment(
                    assessment = assessment,
                    bestPercent = (best * 100).roundToInt(),
                    thresholdPercent = (threshold * 100).roundToInt(),
                    lessonsToReview = moduleReview.map { it.lesson },
                )
            }
        }
        return resume + review + retry
    }
}
