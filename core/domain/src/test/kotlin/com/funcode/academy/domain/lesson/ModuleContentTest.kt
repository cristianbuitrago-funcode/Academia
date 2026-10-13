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
}
