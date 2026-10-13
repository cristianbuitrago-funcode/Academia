package com.funcode.academy.domain.rpg

import com.funcode.academy.domain.mission.MissionDefinition
import com.funcode.academy.domain.mission.MissionProgress
import com.funcode.academy.domain.mission.MissionStatus
import com.funcode.academy.domain.mission.MissionTask
import com.funcode.academy.domain.mission.TaskProgress
import com.funcode.academy.domain.progress.XpAward
import com.funcode.academy.domain.progress.XpSource
import com.funcode.academy.domain.progress.XpSourceType

/** Lo que el contenido permite recompensar (se construye a partir de los JSON). */
data class RewardCatalog(val modules: List<ModuleRewards>, val missions: List<MissionDefinition> = emptyList())

data class ModuleRewards(
    val moduleId: String,
    val lessons: List<LessonRewards>,
    val assessment: AssessmentRewards?,
    val challenge: ChallengeRewards? = null,
)

/** Ejercicios de una lección (no de una evaluación): id → dificultad. */
data class LessonRewards(val lessonId: String, val exerciseDifficulty: Map<String, Int>)

/** Pasos (ejercicios) del reto del módulo. */
data class ChallengeRewards(val challengeId: String, val stepIds: Set<String>)

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
            if (isChallengeCompleted(module, results)) {
                add(XpAward(XpSource(XpSourceType.CHALLENGE, module.challenge!!.challengeId), RewardRules.CHALLENGE_XP))
            }
        }
        missions(catalog, results).filter { it.status == MissionStatus.COMPLETED }.forEach {
            add(XpAward(XpSource(XpSourceType.MISSION, it.mission.id), RewardRules.MISSION_XP))
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
            completedChallenges = catalog.modules.count { isChallengeCompleted(it, results) },
        )
    }

    /**
     * Estado de cada misión, en orden de requisitos. Una misión se completa cuando
     * sus requisitos están completados y todas sus tareas se cumplen con resultados
     * verificados; abrirla no cambia nada.
     */
    fun missions(catalog: RewardCatalog, results: VerifiedResults): List<MissionProgress> {
        val lessons = catalog.modules.flatMap { it.lessons }.associateBy { it.lessonId }
        val exerciseOwner = catalog.modules.flatMap { m ->
            m.lessons.flatMap { l -> l.exerciseDifficulty.keys.map { it to l.lessonId } } +
                m.challenge?.let { c -> c.stepIds.map { it to c.challengeId } }.orEmpty()
        }.toMap()
        val solved = results.firstCorrectHints.keys

        fun taskProgress(task: MissionTask): TaskProgress = when (task) {
            is MissionTask.CompleteLesson -> {
                val ids = lessons[task.lessonId]?.exerciseDifficulty?.keys.orEmpty()
                TaskProgress(task, ids.count { it in solved }, ids.size.coerceAtLeast(1), task.lessonId)
            }
            is MissionTask.SolveExercises -> {
                val next = task.exerciseIds.firstOrNull { it !in solved } ?: task.exerciseIds.first()
                TaskProgress(task, task.exerciseIds.count { it in solved }, task.exerciseIds.size, exerciseOwner[next] ?: next)
            }
            is MissionTask.PassAssessment -> {
                val module = catalog.modules.firstOrNull { it.assessment?.assessmentId == task.assessmentId }
                TaskProgress(task, if (module != null && isAssessmentPassed(module, results)) 1 else 0, 1, task.assessmentId)
            }
            is MissionTask.CompleteChallenge -> {
                val module = catalog.modules.firstOrNull { it.challenge?.challengeId == task.challengeId }
                val steps = module?.challenge?.stepIds.orEmpty()
                // Los pasos solo cuentan con el reto abierto (todas las lecciones completadas).
                val current = if (module != null && isChallengeCompleted(module, results)) steps.size
                else steps.count { it in solved }.coerceAtMost(steps.size - 1).coerceAtLeast(0)
                TaskProgress(task, current, steps.size.coerceAtLeast(1), task.challengeId)
            }
        }

        val byId = catalog.missions.associateBy { it.id }
        val memo = mutableMapOf<String, MissionProgress>()
        fun evaluate(m: MissionDefinition): MissionProgress = memo.getOrPut(m.id) {
            val missing = m.prerequisiteIds.mapNotNull { byId[it] }.filter { evaluate(it).status != MissionStatus.COMPLETED }
            val tasks = m.tasks.map(::taskProgress)
            val status = when {
                missing.isNotEmpty() -> MissionStatus.LOCKED
                tasks.all { it.done } -> MissionStatus.COMPLETED
                tasks.any { it.current > 0 } -> MissionStatus.IN_PROGRESS
                else -> MissionStatus.AVAILABLE
            }
            MissionProgress(m, status, tasks, missing)
        }
        return catalog.missions.map(::evaluate)
    }

    /** El reto cuenta si se resolvieron todos sus pasos estando abierto (todas las lecciones completadas). */
    private fun isChallengeCompleted(module: ModuleRewards, results: VerifiedResults): Boolean {
        val challenge = module.challenge ?: return false
        return challenge.stepIds.isNotEmpty() &&
            challenge.stepIds.all { it in results.firstCorrectHints } &&
            module.lessons.all { isCompleted(it, results) }
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
