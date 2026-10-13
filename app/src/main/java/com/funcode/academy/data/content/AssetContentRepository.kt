package com.funcode.academy.data.content

import android.content.res.AssetManager
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.LessonLoadResult
import com.funcode.academy.domain.lesson.LessonParseResult
import com.funcode.academy.domain.lesson.LessonParser
import com.funcode.academy.domain.lesson.LessonSummary
import com.funcode.academy.domain.lesson.toSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Lee el contenido educativo empaquetado en la app (`assets/content/<idioma>/<lenguaje>`).
 * Funciona sin conexión. Las lecciones se leen una vez y se guardan en memoria.
 */
class AssetContentRepository(
    private val assets: AssetManager,
    private val basePath: String = "content/es/java",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ContentRepository {

    private val mutex = Mutex()
    private var cache: Map<String, LessonParseResult>? = null

    override suspend fun listLessons(): List<LessonSummary> =
        loadAll().values.filterIsInstance<LessonParseResult.Valid>().map { it.lesson.toSummary() }

    override suspend fun loadLesson(lessonId: String): LessonLoadResult = when (val r = loadAll()[lessonId]) {
        null -> LessonLoadResult.NotFound
        is LessonParseResult.Valid -> LessonLoadResult.Loaded(r.lesson)
        is LessonParseResult.Invalid -> LessonLoadResult.Invalid(r.errors)
    }

    /** Lee el índice y todas sus lecciones. Clave: id de la lección (o la ruta si no se pudo leer). */
    private suspend fun loadAll(): Map<String, LessonParseResult> = mutex.withLock {
        cache ?: withContext(ioDispatcher) {
            val paths = try {
                LessonParser.parseIndex(read("$basePath/index.json"))
            } catch (e: IOException) {
                emptyList()
            } catch (e: IllegalArgumentException) {
                emptyList()
            }
            paths.associate { path ->
                val result = try {
                    LessonParser.parse(read("$basePath/$path"))
                } catch (e: IOException) {
                    LessonParseResult.Invalid(listOf("No se pudo leer $path: ${e.message}"))
                }
                val key = (result as? LessonParseResult.Valid)?.lesson?.id ?: idFromPath(path)
                key to result
            }
        }.also { cache = it }
    }

    private fun read(path: String): String = assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    /** `lessons/java.m02.l01.json` → `java.m02.l01`, para poder informar de lecciones inválidas. */
    private fun idFromPath(path: String) = path.substringAfterLast('/').removeSuffix(".json")
}
