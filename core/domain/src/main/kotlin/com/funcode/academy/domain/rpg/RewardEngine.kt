package com.funcode.academy.domain.rpg

import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.LessonLoadResult
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.progress.XpAward
import com.funcode.academy.domain.progress.XpAwardResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Lo que ha cambiado en una sincronización de recompensas. */
data class RewardOutcome(
    val granted: List<XpAward>,
    val newAchievements: List<Achievement>,
    val levelBefore: Int,
    val levelAfter: Int,
) {
    val xpGained: Int get() = granted.sumOf { it.amount }
    val leveledUp: Boolean get() = levelAfter > levelBefore
}

/**
 * Único punto que concede recompensas. No recibe "XP a sumar": vuelve a calcular,
 * a partir de los resultados verificados guardados (intentos correctos y mejores
 * resultados de evaluación), qué recompensas corresponden, y registra las que falten.
 *
 * Por eso es seguro llamarlo cuantas veces se quiera (pulsaciones repetidas,
 * reinicios, fallos a mitad): el registro de XP y la tabla de logros rechazan los
 * duplicados, y lo que no se llegó a guardar se concede en la siguiente llamada.
 */
class RewardEngine(
    private val content: ContentRepository,
    private val progress: ProgressRepository,
) {
    private val mutex = Mutex()
    private var catalog: RewardCatalog? = null

    suspend fun sync(): RewardOutcome = mutex.withLock {
        val catalog = catalog ?: loadCatalog().also { catalog = it }
        val results = VerifiedResults(progress.firstCorrectAttempts(), progress.observeBestAssessmentScores().first())
        val levelBefore = LevelCurve.progressFor(progress.observeTotalXp().first()).level

        val alreadyGranted = progress.observeXpEntries().first().map { it.source }.toSet()
        val granted = RewardCalculator.dueAwards(catalog, results)
            .filter { it.source !in alreadyGranted }
            .filter { progress.award(it) is XpAwardResult.Granted }

        val totalXp = progress.observeTotalXp().first()
        val state = RewardCalculator.learningState(catalog, results, totalXp)
        val unlocked = progress.observeUnlockedAchievements().first().keys
        val newAchievements = Achievement.entries
            .filter { it.id !in unlocked && it.isMet(state) }
            .filter { progress.unlockAchievement(it.id) }

        RewardOutcome(granted, newAchievements, levelBefore, LevelCurve.progressFor(totalXp).level)
    }

    /** Ejercicios de lecciones (con su dificultad) y evaluaciones de cada módulo. */
    private suspend fun loadCatalog(): RewardCatalog = RewardCatalog(
        content.listModules().map { module ->
            ModuleRewards(
                moduleId = module.id,
                lessons = module.lessons.mapNotNull { summary ->
                    (content.loadLesson(summary.id) as? LessonLoadResult.Loaded)?.lesson?.let { lesson ->
                        LessonRewards(lesson.id, lesson.exercises.associate { it.id to it.difficulty })
                    }
                },
                assessment = module.assessment?.let { summary ->
                    (content.loadLesson(summary.id) as? LessonLoadResult.Loaded)?.lesson?.let {
                        AssessmentRewards(it.id, it.assessment?.passThreshold)
                    }
                },
            )
        },
    )
}
