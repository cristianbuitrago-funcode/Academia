package com.funcode.academy.domain.progress

import com.funcode.academy.domain.lesson.CourseModule
import com.funcode.academy.domain.lesson.LessonSummary

/** Avance en una lección, deducido de los ejercicios resueltos. */
data class LessonProgress(val lesson: LessonSummary, val solvedExercises: Int) {
    val totalExercises: Int get() = lesson.exerciseCount

    /** Completada cuando todos sus ejercicios se han resuelto al menos una vez. */
    val completed: Boolean get() = solvedExercises == totalExercises
}

/**
 * Estado del reto del módulo. Se abre cuando todas las lecciones están completadas
 * (decisión del responsable, Fase 6); se supera al resolver todos sus pasos.
 */
data class ChallengeProgress(
    val challenge: LessonSummary,
    val solvedSteps: Int,
    /** Lecciones que faltan por completar para abrirlo (0 = abierto). */
    val lessonsRemaining: Int,
) {
    val totalSteps: Int get() = challenge.exerciseCount
    val unlocked: Boolean get() = lessonsRemaining == 0
    val completed: Boolean get() = totalSteps > 0 && solvedSteps == totalSteps
}

/** Actividad recomendada para continuar. */
sealed interface NextActivity {
    data class Lesson(val lesson: LessonSummary) : NextActivity
    data class Assessment(val assessment: LessonSummary) : NextActivity
    data class Challenge(val challenge: LessonSummary) : NextActivity

    /** Todo el módulo está hecho; se puede repasar o repetir la evaluación o el reto. */
    data object ModuleDone : NextActivity
}

/**
 * Estado de un módulo calculado a partir de los datos guardados (intentos y mejor
 * resultado de la evaluación). No se guarda en ningún sitio: siempre se recalcula,
 * así que abrir varias veces una actividad no puede duplicar su finalización.
 */
data class ModuleProgress(
    val module: CourseModule,
    val lessons: List<LessonProgress>,
    /** Mejor resultado de la evaluación (0–1), o `null` si no se ha hecho. */
    val bestAssessmentScore: Double?,
    val challenge: ChallengeProgress? = null,
) {
    val completedLessons: Int get() = lessons.count { it.completed }

    val next: NextActivity
        get() {
            lessons.firstOrNull { !it.completed }?.let { return NextActivity.Lesson(it.lesson) }
            val assessment = module.assessment
            if (assessment != null && bestAssessmentScore == null) return NextActivity.Assessment(assessment)
            challenge?.takeIf { !it.completed }?.let { return NextActivity.Challenge(it.challenge) }
            return NextActivity.ModuleDone
        }

    companion object {
        fun of(module: CourseModule, solvedExerciseIds: Set<String>, bestScores: Map<String, Double>): ModuleProgress {
            val lessons = module.lessons.map { lesson ->
                LessonProgress(lesson, lesson.exerciseIds.count { it in solvedExerciseIds })
            }
            return ModuleProgress(
                module = module,
                lessons = lessons,
                bestAssessmentScore = bestScores[module.id],
                challenge = module.challenge?.let { challenge ->
                    ChallengeProgress(
                        challenge = challenge,
                        solvedSteps = challenge.exerciseIds.count { it in solvedExerciseIds },
                        lessonsRemaining = lessons.count { !it.completed },
                    )
                },
            )
        }
    }
}
