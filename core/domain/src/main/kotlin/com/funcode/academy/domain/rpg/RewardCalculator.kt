package com.funcode.academy.domain.rpg

import com.funcode.academy.domain.progress.XpAward
import com.funcode.academy.domain.progress.XpSource
import com.funcode.academy.domain.progress.XpSourceType

/** Lo que el contenido permite recompensar (se construye a partir de los JSON). */
data class RewardCatalog(val modules: List<ModuleRewards>)

data class ModuleRewards(val moduleId: String, val lessons: List<LessonRewards>, val assessment: AssessmentRewards?)

/** Ejercicios de una lección (no de una evaluación): id → dificultad. */
data class LessonRewards(val lessonId: String, val exerciseDifficulty: Map<String, Int>)

/** `passThreshold` nulo = sin umbral aprobado: la evaluación no se puede aprobar ni da XP. */
data class AssessmentRewards(val assessmentId: String, val passThreshold: Double?)

/** Resultados verificados guardados en el dispositivo. */
data class VerifiedResults(
    /** Ejercicios resueltos → pistas usadas en su primer acierto. */
    val firstCorrectHints: Map<String, Int>,
    /** Mejor resultado (0–1) de la evaluación de cada módulo. */
    val bestAssessmentScores: Map<String, Double>,
)

/**
 * Calcula, de forma pura y determinista, todas las recompensas que corresponden a
 * los resultados verificados. Volver a calcularlas no cambia nada: el registro de XP
 * rechaza las que ya se concedieron.
 */
object RewardCalculator {

    fun dueAwards(catalog: RewardCatalog, results: VerifiedResults): List<XpAward> = buildList {
        catalog.modules.forEach { module ->
            module.lessons.forEach { lesson ->
                lesson.exerciseDifficulty.forEach { (exerciseId, difficulty) ->
                    results.firstCorrectHints[exerciseId]?.let { hints ->
                        add(XpAward(XpSource(XpSourceType.EXERCISE, exerciseId), RewardRules.exerciseXp(difficulty, hints)))
                    }
                }
                if (isCompleted(lesson, results)) {
                    add(XpAward(XpSource(XpSourceType.LESSON, lesson.lessonId), RewardRules.LESSON_XP))
                }
            }
            if (isAssessmentPassed(module, results)) {
                add(XpAward(XpSource(XpSourceType.ASSESSMENT, module.assessment!!.assessmentId), RewardRules.ASSESSMENT_PASS_XP))
            }
        }
    }

    fun learningState(catalog: RewardCatalog, results: VerifiedResults, totalXp: Int): LearningState {
        val completedLessons = catalog.modules.sumOf { m -> m.lessons.count { isCompleted(it, results) } }
        val passed = catalog.modules.filter { isAssessmentPassed(it, results) }
        return LearningState(
            completedLessons = completedLessons,
            passedAssessments = passed.size,
            masteredModules = passed.count { m -> m.lessons.all { isCompleted(it, results) } },
            level = LevelCurve.progressFor(totalXp).level,
        )
    }

    /** Una lección sin ejercicios no se puede completar (no hay nada que verificar). */
    private fun isCompleted(lesson: LessonRewards, results: VerifiedResults) =
        lesson.exerciseDifficulty.isNotEmpty() && lesson.exerciseDifficulty.keys.all { it in results.firstCorrectHints }

    private fun isAssessmentPassed(module: ModuleRewards, results: VerifiedResults): Boolean {
        val threshold = module.assessment?.passThreshold ?: return false
        val best = results.bestAssessmentScores[module.moduleId] ?: return false
        return best >= threshold
    }
}
