package com.funcode.academy.data.content

import android.content.res.AssetManager
import com.funcode.academy.domain.lesson.ContentIndex
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.CourseModule
import com.funcode.academy.domain.lesson.LessonLoadResult
import com.funcode.academy.domain.lesson.LessonParseResult
import com.funcode.academy.domain.lesson.LessonParser
import com.funcode.academy.domain.lesson.LessonSummary
import com.funcode.academy.domain.lesson.ModuleAssembler
import com.funcode.academy.domain.lesson.ModuleLoadResult
import com.funcode.academy.domain.lesson.ModuleParseResult
import com.funcode.academy.domain.lesson.toSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Lee el contenido educativo empaquetado en la app (`assets/content/<idioma>/<lenguaje>`).
 * Funciona sin conexión. Todo se lee y valida una vez y se guarda en memoria.
 */
class AssetContentRepository(
    private val assets: AssetManager,
    private val basePath: String = "content/es/java",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ContentRepository {

    private class Content(val lessons: Map<String, LessonParseResult>, val modules: Map<String, ModuleLoadResult>)

    private val mutex = Mutex()
    private var cache: Content? = null

    override suspend fun listLessons(): List<LessonSummary> =
        load().lessons.values.filterIsInstance<LessonParseResult.Valid>().map { it.lesson.toSummary() }

    override suspend fun loadLesson(lessonId: String): LessonLoadResult = when (val r = load().lessons[lessonId]) {
        null -> LessonLoadResult.NotFound
        is LessonParseResult.Valid -> LessonLoadResult.Loaded(r.lesson)
        is LessonParseResult.Invalid -> LessonLoadResult.Invalid(r.errors)
    }

    override suspend fun listModules(): List<CourseModule> =
        load().modules.values.filterIsInstance<ModuleLoadResult.Loaded>().map { it.module }.sortedBy { it.order }

    override suspend fun loadModule(moduleId: String): ModuleLoadResult = load().modules[moduleId] ?: ModuleLoadResult.NotFound

    private suspend fun load(): Content = mutex.withLock {
        cache ?: withContext(ioDispatcher) { readAll() }.also { cache = it }
    }

    private fun readAll(): Content {
        val index = try {
            LessonParser.parseIndex(read("$basePath/index.json"))
        } catch (e: IOException) {
            ContentIndex(emptyList(), emptyList())
        } catch (e: IllegalArgumentException) {
            ContentIndex(emptyList(), emptyList())
        }
        val lessons = index.lessons.associate { path ->
            val result = try {
                LessonParser.parse(read("$basePath/$path"))
            } catch (e: IOException) {
                LessonParseResult.Invalid(listOf("No se pudo leer $path: ${e.message}"))
            }
            val key = (result as? LessonParseResult.Valid)?.lesson?.id ?: idFromPath(path)
            key to result
        }
        val validSummaries = lessons.values.filterIsInstance<LessonParseResult.Valid>().associate { it.lesson.id to it.lesson.toSummary() }
        val modules = index.modules.associate { path ->
            val parsed = try {
                LessonParser.parseModule(read("$basePath/$path"))
            } catch (e: IOException) {
                ModuleParseResult.Invalid(listOf("No se pudo leer $path: ${e.message}"))
            }
            when (parsed) {
                is ModuleParseResult.Valid -> parsed.definition.id to ModuleAssembler.assemble(parsed.definition, validSummaries)
                is ModuleParseResult.Invalid -> idFromPath(path) to ModuleLoadResult.Invalid(parsed.errors)
            }
        }
        return Content(lessons, modules)
    }

    private fun read(path: String): String = assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    /** `lessons/java.m01.l01.json` → `java.m01.l01`, para poder informar de archivos inválidos. */
    private fun idFromPath(path: String) = path.substringAfterLast('/').removeSuffix(".json")
}
