package com.funcode.academy.domain.lesson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Comprueba el contenido real del módulo 1 (app/src/main/assets). */
class ModuleContentTest {

    private val module get() = ContentFiles.modules.single { it.id == "java.m01" }

    @Test fun `todas las lecciones y modulos del indice son validos`() {
        assertEquals(9, ContentFiles.lessons.size)
        assertEquals(1, ContentFiles.modules.size)
    }

    @Test fun `el modulo 1 tiene siete lecciones en orden y una evaluacion`() {
        assertEquals((1..7).map { "java.m01.l%02d".format(it) }, module.lessons.map { it.id })
        assertEquals("java.m01.eval", module.assessment?.id)
    }

    @Test fun `el reto del modulo es un borrador guiado con pistas, dificultad creciente y todos los objetivos practicados`() {
        assertEquals("java.m01.reto", module.challenge?.id)
        val challenge = ContentFiles.lessons.single { it.id == "java.m01.reto" }
        assertEquals(LessonKind.CHALLENGE, challenge.kind)
        assertEquals(ReviewStatus.DRAFT, challenge.reviewStatus)
        assertEquals(null, challenge.assessment)
        assertTrue(challenge.exercises.size >= 5)
        assertTrue(challenge.exercises.all { it.hints.isNotEmpty() })
        val difficulties = challenge.exercises.map { it.difficulty }
        assertEquals(difficulties.sorted(), difficulties)
        assertTrue(challenge.exercises.map { it.content::class }.toSet().size >= 4)
        challenge.objectives.forEach { o -> assertTrue(o.id, challenge.exercises.any { o.id in it.objectiveIds }) }
    }

    @Test fun `todo el contenido nuevo sigue en borrador`() {
        assertEquals(ReviewStatus.DRAFT, module.reviewStatus)
        ContentFiles.lessons.forEach { assertEquals(it.id, ReviewStatus.DRAFT, it.reviewStatus) }
    }

    @Test fun `cada leccion tiene objetivos, teoria, un ejemplo de codigo comentado, pistas y dificultad creciente`() {
        ContentFiles.lessons.filter { it.kind == LessonKind.LESSON }.forEach { lesson ->
            val sections = lesson.sectionsFor(null)
            assertTrue(lesson.id, lesson.objectives.isNotEmpty())
            assertTrue(lesson.id, sections.any { it is Section.Text })
            assertTrue(lesson.id, sections.filterIsInstance<Section.Code>().any { it.lineComments.isNotEmpty() })
            assertTrue(lesson.id, lesson.exercises.size >= 5)
            assertTrue(lesson.id, lesson.exercises.all { it.hints.isNotEmpty() })
            val difficulties = lesson.exercises.map { it.difficulty }
            assertEquals(lesson.id, difficulties.sorted(), difficulties)
        }
    }

    @Test fun `la evaluacion usa varios tipos, cubre todos sus objetivos y no define umbral mientras P6 este pendiente`() {
        val evaluation = ContentFiles.lessons.single { it.id == "java.m01.eval" }
        assertEquals(LessonKind.ASSESSMENT, evaluation.kind)
        assertEquals(10, evaluation.exercises.size)
        assertTrue(evaluation.exercises.map { it.content::class }.toSet().size >= 6)
        evaluation.objectives.forEach { o -> assertTrue(o.id, evaluation.exercises.any { o.id in it.objectiveIds }) }
        assertNotNull(evaluation.assessment)
        // Umbral aprobado por el responsable (P6): 80 %.
        assertEquals(0.8, evaluation.assessment!!.passThreshold!!, 0.0)
    }

    @Test fun `el modulo usa los siete tipos de ejercicio`() {
        val types = ContentFiles.lessons.flatMap { it.exercises }.map { it.content::class }.toSet()
        assertEquals(7, types.size)
    }

    @Test fun `la respuesta declarada de cada ejercicio se valida como correcta`() {
        ContentFiles.lessons.flatMap { it.exercises }.forEach { e ->
            assertEquals(e.id, AnswerResult.Correct, AnswerValidator.validate(e.content, TestLessons.correctAnswerFor(e.content)))
        }
    }

    @Test fun `los identificadores de ejercicio son unicos en todo el contenido`() {
        val ids = ContentFiles.lessons.flatMap { it.exercises }.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun `el proyecto guiado tiene situacion, conceptos previos, pistas progresivas y resumen`() {
        val project = ContentFiles.lessons.single { it.id == "java.m01.reto" }
        val info = project.project!!
        assertTrue(info.situation.isNotBlank())
        assertTrue(info.priorConcepts.size >= 2)
        assertTrue(info.recap.size >= 2)
        assertTrue(project.exercises.all { it.hints.size >= 2 })
    }

    @Test fun `las misiones son borradores validos que cubren las siete lecciones y los cuatro tipos`() {
        val missions = ContentFiles.missions
        assertEquals((1..7).map { "java.m01.mi0$it" }, missions.map { it.id })
        assertTrue(missions.all { it.reviewStatus == ReviewStatus.DRAFT && it.version >= 1 })
        assertEquals(com.funcode.academy.domain.mission.MissionType.entries.toSet(), missions.map { it.type }.toSet())
        val lessons = missions.flatMap { it.tasks }.filterIsInstance<com.funcode.academy.domain.mission.MissionTask.CompleteLesson>()
            .map { it.lessonId }
        assertEquals((1..7).map { "java.m01.l0$it" }, lessons.sorted())
        // Solo la primera está disponible sin requisitos.
        assertEquals(listOf("java.m01.mi01"), missions.filter { it.prerequisiteIds.isEmpty() }.map { it.id })
    }

    @Test fun `la leccion 1 tiene variantes para los tres publicos con el mismo ejemplo y los mismos ejercicios`() {
        val lesson = ContentFiles.lessons.single { it.id == "java.m01.l01" }
        assertEquals(PresentationVariant.entries.toSet(), lesson.presentations.keys)
        val sections = com.funcode.academy.domain.model.Audience.entries.map { lesson.sectionsFor(it) } + listOf(lesson.sectionsFor(null))
        // Cada variante cambia el texto, pero conserva el ejemplo del robot al que se refieren los ejercicios.
        assertEquals(sections.size, sections.toSet().size)
        val codes = sections.map { s -> s.filterIsInstance<Section.Code>().single().code }.toSet()
        assertEquals(1, codes.size)
        // Los objetivos y los ejercicios son únicos para la lección: no dependen de la variante.
        assertEquals(2, lesson.objectives.size)
    }

    @Test fun `una leccion sin variantes usa la presentacion comun para cualquier publico`() {
        val lesson = ContentFiles.lessons.single { it.id == "java.m01.l05" }
        com.funcode.academy.domain.model.Audience.entries.forEach {
            assertEquals(lesson.sectionsFor(null), lesson.sectionsFor(it))
        }
    }
}
