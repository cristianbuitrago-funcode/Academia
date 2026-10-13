package com.funcode.academy.domain.mission

import com.funcode.academy.domain.lesson.ReviewStatus
import com.funcode.academy.domain.progress.XpSource
import com.funcode.academy.domain.progress.XpSourceType
import com.funcode.academy.domain.rpg.AssessmentRewards
import com.funcode.academy.domain.rpg.ChallengeRewards
import com.funcode.academy.domain.rpg.LessonRewards
import com.funcode.academy.domain.rpg.ModuleRewards
import com.funcode.academy.domain.rpg.RewardCalculator
import com.funcode.academy.domain.rpg.RewardCatalog
import com.funcode.academy.domain.rpg.RewardRules
import com.funcode.academy.domain.rpg.VerifiedResults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Estados y transiciones de las misiones (cálculo puro). */
class MissionProgressTest {

    private fun mission(id: String, order: Int, vararg tasks: MissionTask, prerequisites: List<String> = emptyList()) =
        MissionDefinition(id, "m", order, 1, ReviewStatus.DRAFT, MissionType.LEARNING, id, "D", "O", prerequisites, tasks.toList())

    private val learn = mission("m.mi1", 1, MissionTask.CompleteLesson("m.l1"))
    private val practice = mission(
        "m.mi2", 2,
        MissionTask.SolveExercises(listOf("m.l2.e1", "m.reto.s1"), "D"),
        prerequisites = listOf("m.mi1"),
    )
    private val evaluation = mission("m.mi3", 3, MissionTask.PassAssessment("m.eval"), prerequisites = listOf("m.mi2"))
    private val project = mission("m.mi4", 4, MissionTask.CompleteChallenge("m.reto"), prerequisites = listOf("m.mi1"))

    private val catalog = RewardCatalog(
        modules = listOf(
            ModuleRewards(
                "m",
                listOf(LessonRewards("m.l1", mapOf("m.l1.e1" to 1, "m.l1.e2" to 1)), LessonRewards("m.l2", mapOf("m.l2.e1" to 1))),
                AssessmentRewards("m.eval", 0.8),
                ChallengeRewards("m.reto", setOf("m.reto.s1", "m.reto.s2")),
            ),
        ),
        missions = listOf(learn, practice, evaluation, project),
    )

    private fun states(solved: Set<String>, scores: Map<String, Double> = emptyMap()) =
        RewardCalculator.missions(catalog, VerifiedResults(solved.associateWith { 0 }, scores))

    @Test fun `estado inicial`() {
        val m = states(emptySet())
        assertEquals(listOf(MissionStatus.AVAILABLE, MissionStatus.LOCKED, MissionStatus.LOCKED, MissionStatus.LOCKED), m.map { it.status })
        assertEquals(listOf(learn), m[1].missingPrerequisites)
        assertEquals(listOf(practice), m[2].missingPrerequisites)
    }

    @Test fun `transiciones disponible, en progreso y completada`() {
        assertEquals(MissionStatus.IN_PROGRESS, states(setOf("m.l1.e1"))[0].status)
        val done = states(setOf("m.l1.e1", "m.l1.e2"))
        assertEquals(MissionStatus.COMPLETED, done[0].status)
        assertEquals(MissionStatus.AVAILABLE, done[1].status)
        assertEquals(TaskProgress(MissionTask.CompleteLesson("m.l1"), 2, 2, "m.l1"), done[0].tasks.single())
    }

    @Test fun `una mision bloqueada no se completa aunque sus tareas esten hechas`() {
        val m = states(setOf("m.l2.e1", "m.reto.s1"))
        assertEquals(MissionStatus.LOCKED, m[1].status)
        val awards = RewardCalculator.dueAwards(catalog, VerifiedResults(mapOf("m.l2.e1" to 0, "m.reto.s1" to 0), emptyMap()))
        assertTrue(awards.none { it.source.type == XpSourceType.MISSION })
        // Al cumplir el requisito pasa directamente a completada.
        assertEquals(MissionStatus.COMPLETED, states(setOf("m.l1.e1", "m.l1.e2", "m.l2.e1", "m.reto.s1"))[1].status)
    }

    @Test fun `la tarea indica la actividad que hay que abrir`() {
        val tasks = states(setOf("m.l1.e1", "m.l1.e2", "m.l2.e1"))[1].tasks.single()
        assertEquals("m.reto", tasks.activityId) // el siguiente ejercicio pendiente es un paso del reto
        assertEquals(1, tasks.current)
    }

    @Test fun `evaluacion por debajo y por encima del umbral`() {
        val ready = setOf("m.l1.e1", "m.l1.e2", "m.l2.e1", "m.reto.s1")
        assertEquals(MissionStatus.AVAILABLE, states(ready, mapOf("m" to 0.7))[2].status)
        assertEquals(MissionStatus.COMPLETED, states(ready, mapOf("m" to 0.8))[2].status)
    }

    @Test fun `el proyecto solo cuenta con el reto abierto y completo`() {
        val stepsOnly = states(setOf("m.l1.e1", "m.l1.e2", "m.reto.s1", "m.reto.s2"))[3]
        assertEquals(MissionStatus.IN_PROGRESS, stepsOnly.status) // falta la lección 2 para abrir el reto
        assertEquals(1, stepsOnly.tasks.single().current)
        val all = states(setOf("m.l1.e1", "m.l1.e2", "m.l2.e1", "m.reto.s1", "m.reto.s2"))[3]
        assertEquals(MissionStatus.COMPLETED, all.status)
    }

    @Test fun `cada mision completada da su bonificacion una vez`() {
        val awards = RewardCalculator.dueAwards(catalog, VerifiedResults(mapOf("m.l1.e1" to 0, "m.l1.e2" to 0), emptyMap()))
        assertEquals(1, awards.count { it.source == XpSource(XpSourceType.MISSION, "m.mi1") })
        assertEquals(RewardRules.MISSION_XP, awards.single { it.source.type == XpSourceType.MISSION }.amount)
    }
}
