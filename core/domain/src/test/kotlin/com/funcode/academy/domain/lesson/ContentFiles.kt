package com.funcode.academy.domain.lesson

import com.funcode.academy.domain.mission.MissionAssembler
import com.funcode.academy.domain.mission.MissionDefinition
import com.funcode.academy.domain.mission.MissionParseResult
import java.io.File

/** Acceso al contenido real de la app desde las pruebas del dominio (sin Android). */
internal object ContentFiles {
    val root = File("../../app/src/main/assets/content/es/java")

    val index: ContentIndex by lazy { LessonParser.parseIndex(File(root, "index.json").readText()) }

    val lessons: List<Lesson> by lazy {
        index.lessons.map { path ->
            when (val r = LessonParser.parse(File(root, path).readText())) {
                is LessonParseResult.Valid -> r.lesson
                is LessonParseResult.Invalid -> throw AssertionError("$path no es válida: ${r.errors}")
            }
        }
    }

    val modules: List<CourseModule> by lazy {
        val summaries = lessons.associate { it.id to it.toSummary() }
        index.modules.map { path ->
            val definition = when (val r = LessonParser.parseModule(File(root, path).readText())) {
                is ModuleParseResult.Valid -> r.definition
                is ModuleParseResult.Invalid -> throw AssertionError("$path no es válido: ${r.errors}")
            }
            when (val m = ModuleAssembler.assemble(definition, summaries)) {
                is ModuleLoadResult.Loaded -> m.module
                else -> throw AssertionError("$path: $m")
            }
        }
    }

    val missions: List<MissionDefinition> by lazy {
        val definitions = index.missions.map { path ->
            when (val r = LessonParser.parseMission(File(root, path).readText())) {
                is MissionParseResult.Valid -> r.mission
                is MissionParseResult.Invalid -> throw AssertionError("$path no es válida: ${r.errors}")
            }
        }
        val set = MissionAssembler.assemble(definitions, lessons.associate { it.id to it.toSummary() })
        if (set.errors.isNotEmpty()) throw AssertionError("Misiones no válidas: ${set.errors}")
        set.missions
    }
}
