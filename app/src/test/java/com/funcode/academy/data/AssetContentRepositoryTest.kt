package com.funcode.academy.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.funcode.academy.data.content.AssetContentRepository
import com.funcode.academy.domain.lesson.LessonLoadResult
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
    fun listaLaLeccionDemostrativaComoBorrador() = runTest {
        val lesson = repository.listLessons().single { it.id == "java.m02.l01" }
        assertEquals(ReviewStatus.DRAFT, lesson.reviewStatus)
        assertEquals(7, lesson.exerciseCount)
    }

    @Test
    fun cargaLaLeccionCompleta() = runTest {
        val result = repository.loadLesson("java.m02.l01")
        assertTrue(result is LessonLoadResult.Loaded)
        assertEquals(7, (result as LessonLoadResult.Loaded).lesson.exercises.size)
    }

    @Test
    fun unaLeccionInexistenteNoSeEncuentra() = runTest {
        assertEquals(LessonLoadResult.NotFound, repository.loadLesson("java.m99.l99"))
    }

    @Test
    fun unaRutaDeContenidoInexistenteNoRompeLaApp() = runTest {
        val empty = AssetContentRepository(ApplicationProvider.getApplicationContext<Context>().assets, basePath = "content/xx/nada")
        assertTrue(empty.listLessons().isEmpty())
        assertEquals(LessonLoadResult.NotFound, empty.loadLesson("java.m02.l01"))
    }
}
