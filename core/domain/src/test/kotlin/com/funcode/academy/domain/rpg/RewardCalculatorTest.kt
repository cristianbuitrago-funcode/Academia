package com.funcode.academy.domain.rpg

import com.funcode.academy.domain.progress.XpAward
import com.funcode.academy.domain.progress.XpSource
import com.funcode.academy.domain.progress.XpSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardCalculatorTest {

    private val catalog = RewardCatalog(
        listOf(
            ModuleRewards(
                moduleId = "m1",
                lessons = listOf(
                    LessonRewards("m1.l1", mapOf("m1.l1.e1" to 1, "m1.l1.e2" to 3)),
                    LessonRewards("m1.l2", mapOf("m1.l2.e1" to 2)),
                ),
                assessment = AssessmentRewards("m1.eval", passThreshold = 0.8),
            ),
        ),
    )

    private fun results(hints: Map<String, Int> = emptyMap(), scores: Map<String, Double> = emptyMap()) =
        VerifiedResults(hints, scores)

    private fun award(type: XpSourceType, id: String, amount: Int) = XpAward(XpSource(type, id), amount)

    @Test fun `sin resultados no hay recompensas`() {
        assertTrue(RewardCalculator.dueAwards(catalog, results()).isEmpty())
    }

    @Test fun `un ejercicio resuelto da su XP con la penalizacion de sus pistas`() {
        val due = RewardCalculator.dueAwards(catalog, results(mapOf("m1.l1.e2" to 1)))
        assertEquals(listOf(award(XpSourceType.EXERCISE, "m1.l1.e2", 15)), due)
    }

    @Test fun `completar todos los ejercicios de una leccion la recompensa`() {
        val due = RewardCalculator.dueAwards(catalog, results(mapOf("m1.l1.e1" to 0, "m1.l1.e2" to 0)))
        assertEquals(
            listOf(
                award(XpSourceType.EXERCISE, "m1.l1.e1", 10),
                award(XpSourceType.EXERCISE, "m1.l1.e2", 20),
                award(XpSourceType.LESSON, "m1.l1", RewardRules.LESSON_XP),
            ),
            due,
        )
    }

    @Test fun `la evaluacion solo da XP si alcanza el umbral`() {
        assertTrue(RewardCalculator.dueAwards(catalog, results(scores = mapOf("m1" to 0.7))).isEmpty())
        assertEquals(
            listOf(award(XpSourceType.ASSESSMENT, "m1.eval", RewardRules.ASSESSMENT_PASS_XP)),
            RewardCalculator.dueAwards(catalog, results(scores = mapOf("m1" to 0.8))),
        )
    }

    @Test fun `sin umbral aprobado la evaluacion no da XP`() {
        val noThreshold = RewardCatalog(listOf(catalog.modules.single().copy(assessment = AssessmentRewards("m1.eval", null))))
        assertTrue(RewardCalculator.dueAwards(noThreshold, results(scores = mapOf("m1" to 1.0))).isEmpty())
    }

    @Test fun `ejercicios ajenos al catalogo se ignoran`() {
        // p. ej. preguntas de evaluación o lecciones retiradas
        assertTrue(RewardCalculator.dueAwards(catalog, results(mapOf("m1.eval.q01" to 0, "java.m02.l01.e01" to 0))).isEmpty())
    }

    @Test fun `una leccion sin ejercicios no cuenta como completada`() {
        val empty = RewardCatalog(listOf(ModuleRewards("m", listOf(LessonRewards("m.l", emptyMap())), null)))
        assertTrue(RewardCalculator.dueAwards(empty, results()).isEmpty())
        assertEquals(0, RewardCalculator.learningState(empty, results(), 0).completedLessons)
    }

    @Test fun `estado de aprendizaje para los logros`() {
        val all = results(mapOf("m1.l1.e1" to 0, "m1.l1.e2" to 2, "m1.l2.e1" to 0), mapOf("m1" to 0.9))
        assertEquals(
            LearningState(completedLessons = 2, passedAssessments = 1, masteredModules = 1, level = 2),
            RewardCalculator.learningState(catalog, all, totalXp = 150),
        )
        val noEval = results(mapOf("m1.l1.e1" to 0, "m1.l1.e2" to 0))
        assertEquals(
            LearningState(completedLessons = 1, passedAssessments = 0, masteredModules = 0, level = 1),
            RewardCalculator.learningState(catalog, noEval, totalXp = 50),
        )
    }

    private val withChallenge = RewardCatalog(
        listOf(catalog.modules.single().copy(challenge = ChallengeRewards("m1.reto", setOf("m1.reto.s1", "m1.reto.s2")))),
    )
    private val allLessons = mapOf("m1.l1.e1" to 0, "m1.l1.e2" to 0, "m1.l2.e1" to 0)
    private val allSteps = mapOf("m1.reto.s1" to 1, "m1.reto.s2" to 0)

    @Test fun `el reto da 50 XP solo con todas sus partes y las lecciones completadas`() {
        val reto = award(XpSourceType.CHALLENGE, "m1.reto", RewardRules.CHALLENGE_XP)
        // Los pasos no dan XP por separado.
        assertTrue(RewardCalculator.dueAwards(withChallenge, results(mapOf("m1.reto.s1" to 0))).isEmpty())
        // Pasos resueltos sin las lecciones completadas: no cuenta.
        assertTrue(reto !in RewardCalculator.dueAwards(withChallenge, results(allSteps)))
        assertTrue(reto in RewardCalculator.dueAwards(withChallenge, results(allLessons + allSteps)))
        assertEquals(1, RewardCalculator.learningState(withChallenge, results(allLessons + allSteps), 0).completedChallenges)
    }

    @Test fun `misiones sin actividad`() {
        val missions = RewardCalculator.missions(withChallenge, results())
        assertEquals(Mission.entries, missions.map { it.mission })
        assertTrue(missions.all { it.current == 0 && !it.completed })
    }

    @Test fun `misiones a partir de resultados verificados`() {
        val missions = RewardCalculator.missions(
            withChallenge,
            results(allLessons + allSteps + mapOf("m1.l1.e2" to 2), scores = mapOf("m1" to 0.8)),
        ).associate { it.mission to it.current }
        // Sin pistas: m1.l1.e1, m1.l2.e1 y m1.reto.s2 (m1.l1.e2 usó pistas).
        assertEquals(3, missions[Mission.SOLVE_5_NO_HINTS])
        assertEquals(3, missions[Mission.SOLVE_20_EXERCISES])
        assertEquals(2, missions[Mission.COMPLETE_5_LESSONS])
        assertEquals(1, missions[Mission.PASS_ASSESSMENT])
        assertEquals(1, missions[Mission.COMPLETE_CHALLENGE])
    }

    @Test fun `el progreso de una mision nunca supera su meta`() {
        val many = (1..30).associate { "x$it" to 0 }
        val catalog30 = RewardCatalog(listOf(ModuleRewards("m", listOf(LessonRewards("m.l", many.mapValues { 1 })), null)))
        val solve = RewardCalculator.missions(catalog30, results(many)).single { it.mission == Mission.SOLVE_20_EXERCISES }
        assertEquals(20, solve.current)
        assertTrue(solve.completed)
        assertEquals(1f, solve.fraction)
    }
}
