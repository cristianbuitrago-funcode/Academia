package com.funcode.academy.domain.mission

import com.funcode.academy.domain.lesson.LessonKind
import com.funcode.academy.domain.lesson.LessonSummary
import com.funcode.academy.domain.lesson.ReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionAssemblerTest {

    private fun summary(id: String, kind: LessonKind, module: String = "m") =
        LessonSummary(id, module, id, "d", ReviewStatus.DRAFT, kind, listOf("$id.e1"))

    private val lessons = listOf(
        summary("m.l1", LessonKind.LESSON),
        summary("m.eval", LessonKind.ASSESSMENT),
        summary("m.reto", LessonKind.CHALLENGE),
        summary("x.l1", LessonKind.LESSON, module = "x"),
    ).associateBy { it.id }

    private fun mission(id: String, vararg tasks: MissionTask, prerequisites: List<String> = emptyList(), order: Int = 1) =
        MissionDefinition(id, "m", order, 1, ReviewStatus.DRAFT, MissionType.LEARNING, "T", "D", "O", prerequisites, tasks.toList())

    private val lesson = MissionTask.CompleteLesson("m.l1")

    @Test fun `misiones validas ordenadas`() {
        val set = MissionAssembler.assemble(
            listOf(
                mission("m.b", MissionTask.CompleteChallenge("m.reto"), prerequisites = listOf("m.a"), order = 2),
                mission("m.a", lesson, MissionTask.SolveExercises(listOf("m.l1.e1", "m.reto.e1"), "D"), MissionTask.PassAssessment("m.eval")),
            ),
            lessons,
        )
        assertEquals(emptyList<String>(), set.errors)
        assertEquals(listOf("m.a", "m.b"), set.missions.map { it.id })
    }

    @Test fun `referencias inexistentes, de otro tipo o de otro modulo`() {
        val set = MissionAssembler.assemble(
            listOf(
                mission("m.a", MissionTask.CompleteLesson("m.nada")),
                mission("m.b", MissionTask.CompleteLesson("m.eval")),
                mission("m.c", MissionTask.CompleteLesson("x.l1")),
                mission("m.d", MissionTask.SolveExercises(listOf("m.eval.e1"), "Las preguntas de evaluación no cuentan")),
                mission("m.e", lesson, prerequisites = listOf("m.fantasma")),
            ),
            lessons,
        )
        assertTrue(set.missions.isEmpty())
        assertTrue(set.errors.any { "inexistente" in it && "m.nada" in it })
        assertTrue(set.errors.any { "LESSON" in it })
        assertTrue(set.errors.any { "otro módulo" in it })
        assertTrue(set.errors.any { "m.eval.e1" in it })
        assertTrue(set.errors.any { "m.fantasma" in it })
    }

    @Test fun `ciclos y dependencias de misiones descartadas`() {
        val set = MissionAssembler.assemble(
            listOf(
                mission("m.a", lesson, prerequisites = listOf("m.b")),
                mission("m.b", lesson, prerequisites = listOf("m.a")),
                mission("m.c", MissionTask.CompleteLesson("m.nada")),
                mission("m.d", lesson, prerequisites = listOf("m.c")),
                mission("m.ok", lesson),
            ),
            lessons,
        )
        assertEquals(listOf("m.ok"), set.missions.map { it.id })
        assertTrue(set.errors.any { "ciclo" in it })
        assertTrue(set.errors.any { "m.d" in it && "no válida" in it })
    }

    @Test fun `ids repetidos`() {
        val set = MissionAssembler.assemble(listOf(mission("m.a", lesson), mission("m.a", lesson)), lessons)
        assertTrue(set.errors.any { "repetida" in it })
        assertEquals(1, set.missions.size)
    }
}
