package com.funcode.academy.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.funcode.academy.data.content.AssetContentRepository
import com.funcode.academy.domain.lesson.LessonKind
import com.funcode.academy.domain.lesson.LessonLoadResult
import com.funcode.academy.domain.lesson.ModuleLoadResult
import com.funcode.academy.domain.lesson.ReviewStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Lee el contenido real empaquetado en `assets` (Robolectric). */
@RunWith(AndroidJUnit4::class)
class AssetContentRepositoryTest {

    private val repository = AssetContentRepository(ApplicationProvider.getApplicationContext<Context>().assets)

    @Test
    fun elModulo1TieneSieteLeccionesEnOrdenUnaEvaluacionYUnReto() = runTest {
        val module = repository.listModules().single()
        assertEquals("java.m01", module.id)
        assertEquals(ReviewStatus.DRAFT, module.reviewStatus)
        assertEquals((1..7).map { "java.m01.l0$it" }, module.lessons.map { it.id })
        assertTrue(module.lessons.all { it.kind == LessonKind.LESSON && it.reviewStatus == ReviewStatus.DRAFT })
        assertEquals("java.m01.eval", module.assessment?.id)
        assertEquals(LessonKind.ASSESSMENT, module.assessment?.kind)
        assertEquals(10, module.assessment?.exerciseCount)
        assertEquals("java.m01.reto", module.challenge?.id)
        assertEquals(LessonKind.CHALLENGE, module.challenge?.kind)
        assertEquals(7, module.challenge?.exerciseCount)
    }

    @Test
    fun cargaElModuloPorSuId() = runTest {
        val result = repository.loadModule("java.m01")
        assertTrue(result is ModuleLoadResult.Loaded)
        assertEquals(ModuleLoadResult.NotFound, repository.loadModule("java.m99"))
    }

    @Test
    fun cargaLaLeccionCompleta() = runTest {
        val result = repository.loadLesson("java.m01.l05")
        assertTrue(result is LessonLoadResult.Loaded)
        assertEquals(7, (result as LessonLoadResult.Loaded).lesson.exercises.size)
    }

    @Test
    fun cargaLaEvaluacionConElUmbralAprobado() = runTest {
        val lesson = (repository.loadLesson("java.m01.eval") as LessonLoadResult.Loaded).lesson
        assertEquals(LessonKind.ASSESSMENT, lesson.kind)
        // Umbral aprobado por el responsable (P6): 80 %.
        assertEquals(0.8, lesson.assessment?.passThreshold)
    }

    @Test
    fun unaLeccionInexistenteNoSeEncuentra() = runTest {
        assertEquals(LessonLoadResult.NotFound, repository.loadLesson("java.m99.l99"))
    }

    @Test
    fun laLeccionAntiguaRenumeradaYaNoExiste() = runTest {
        assertEquals(LessonLoadResult.NotFound, repository.loadLesson("java.m02.l01"))
    }

    @Test
    fun unaRutaDeContenidoInexistenteNoRompeLaApp() = runTest {
        val empty = AssetContentRepository(ApplicationProvider.getApplicationContext<Context>().assets, basePath = "content/xx/nada")
        assertTrue(empty.listLessons().isEmpty())
        assertTrue(empty.listModules().isEmpty())
        assertEquals(LessonLoadResult.NotFound, empty.loadLesson("java.m01.l01"))
        assertEquals(ModuleLoadResult.NotFound, empty.loadModule("java.m01"))
    }
}
