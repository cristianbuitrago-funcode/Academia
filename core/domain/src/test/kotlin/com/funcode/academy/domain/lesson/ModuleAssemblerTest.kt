package com.funcode.academy.domain.lesson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleAssemblerTest {

    private fun summary(id: String, kind: LessonKind, moduleId: String = "m") =
        LessonSummary(id, moduleId, id, "d", ReviewStatus.DRAFT, kind, listOf("$id.e1"))

    private val lessons = listOf(
        summary("m.l1", LessonKind.LESSON),
        summary("m.eval", LessonKind.ASSESSMENT),
        summary("m.reto", LessonKind.CHALLENGE),
        summary("otro.l1", LessonKind.LESSON, moduleId = "otro"),
    ).associateBy { it.id }

    private fun definition(lessonIds: List<String> = listOf("m.l1"), assessment: String? = "m.eval", challenge: String? = "m.reto") =
        ModuleDefinition("m", 1, "M", "D", ReviewStatus.DRAFT, lessonIds, assessment, challenge)

    private fun errors(d: ModuleDefinition) = (ModuleAssembler.assemble(d, lessons) as ModuleLoadResult.Invalid).errors

    @Test fun `monta un modulo con leccion, evaluacion y reto`() {
        val module = (ModuleAssembler.assemble(definition(), lessons) as ModuleLoadResult.Loaded).module
        assertEquals(listOf("m.l1"), module.lessons.map { it.id })
        assertEquals("m.eval", module.assessment?.id)
        assertEquals("m.reto", module.challenge?.id)
    }

    @Test fun `el reto debe existir, ser de tipo reto y del mismo modulo`() {
        assertTrue(errors(definition(challenge = "m.nada")).any { "inexistente" in it })
        assertTrue(errors(definition(challenge = "m.l1")).any { "CHALLENGE" in it })
        assertTrue(errors(definition(challenge = "otro.l1")).any { "pertenece" in it })
    }

    @Test fun `una leccion repetida o de otro tipo se rechaza`() {
        assertTrue(errors(definition(lessonIds = listOf("m.l1", "m.l1"))).any { "repite" in it })
        assertTrue(errors(definition(lessonIds = listOf("m.reto"), challenge = null)).any { "LESSON" in it })
    }
}
