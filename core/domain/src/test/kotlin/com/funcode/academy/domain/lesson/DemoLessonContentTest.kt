package com.funcode.academy.domain.lesson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Comprueba el contenido real de la app (app/src/main/assets). Se ejecuta en
 * :core:domain para que funcione también sin Android SDK.
 */
class DemoLessonContentTest {

    private val contentRoot = File("../../app/src/main/assets/content/es/java")

    private fun lessons(): List<Lesson> =
        LessonParser.parseIndex(File(contentRoot, "index.json").readText()).map { path ->
            when (val r = LessonParser.parse(File(contentRoot, path).readText())) {
                is LessonParseResult.Valid -> r.lesson
                is LessonParseResult.Invalid -> throw AssertionError("$path no es válida: ${r.errors}")
            }
        }

    @Test fun `todas las lecciones del indice son validas`() = assertTrue(lessons().isNotEmpty())

    @Test fun `la leccion demostrativa sigue en borrador`() =
        assertEquals(ReviewStatus.DRAFT, lessons().single { it.id == "java.m02.l01" }.reviewStatus)

    @Test fun `la leccion demostrativa usa los siete tipos de ejercicio`() {
        val types = lessons().single { it.id == "java.m02.l01" }.exercises.map { it.content::class }.toSet()
        assertEquals(7, types.size)
    }

    @Test fun `la dificultad no disminuye a lo largo de la leccion`() {
        val difficulties = lessons().single { it.id == "java.m02.l01" }.exercises.map { it.difficulty }
        assertEquals(difficulties.sorted(), difficulties)
    }

    @Test fun `la respuesta declarada de cada ejercicio se valida como correcta`() {
        lessons().flatMap { it.exercises }.forEach { e ->
            assertEquals(e.id, AnswerResult.Correct, AnswerValidator.validate(e.content, TestLessons.correctAnswerFor(e.content)))
        }
    }
}
