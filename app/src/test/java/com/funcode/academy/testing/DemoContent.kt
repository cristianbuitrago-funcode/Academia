package com.funcode.academy.testing

import com.funcode.academy.domain.lesson.Answer
import com.funcode.academy.domain.lesson.CourseModule
import com.funcode.academy.domain.lesson.ExerciseContent
import com.funcode.academy.domain.lesson.Lesson
import com.funcode.academy.domain.lesson.LessonParseResult
import com.funcode.academy.domain.lesson.LessonParser
import com.funcode.academy.domain.lesson.ModuleAssembler
import com.funcode.academy.domain.lesson.ModuleLoadResult
import com.funcode.academy.domain.lesson.ModuleParseResult
import com.funcode.academy.domain.lesson.toSummary
import java.io.File

/** Contenido real de la app, leído del disco (las pruebas JVM se ejecutan en el directorio del módulo). */
object DemoContent {
    const val LESSON_ID = "java.m01.l05"
    const val ASSESSMENT_ID = "java.m01.eval"
    private const val ROOT = "src/main/assets/content/es/java"

    fun lesson(id: String = LESSON_ID): Lesson {
        val text = File("$ROOT/lessons/$id.json").readText()
        return (LessonParser.parse(text) as LessonParseResult.Valid).lesson
    }

    fun assessment(): Lesson = lesson(ASSESSMENT_ID)

    /** Todas las lecciones del índice real. */
    fun allLessons(): List<Lesson> = LessonParser.parseIndex(File("$ROOT/index.json").readText()).lessons
        .map { path -> (LessonParser.parse(File("$ROOT/$path").readText()) as LessonParseResult.Valid).lesson }

    /** El módulo 1 real, montado con sus lecciones. */
    fun module(): CourseModule {
        val definition = (LessonParser.parseModule(File("$ROOT/modules/java.m01.json").readText()) as ModuleParseResult.Valid).definition
        val summaries = allLessons().associate { it.id to it.toSummary() }
        return (ModuleAssembler.assemble(definition, summaries) as ModuleLoadResult.Loaded).module
    }

    /** Repositorio de contenido con todo el contenido real (lecciones y módulo). */
    fun repository() = FakeContentRepository(allLessons(), listOf(module()))

    fun correctAnswerFor(content: ExerciseContent): Answer = when (content) {
        is ExerciseContent.MultipleChoice -> Answer.Choice(content.correctIndices)
        is ExerciseContent.TrueFalse -> Answer.TrueFalse(content.correct)
        is ExerciseContent.FillInBlanks -> Answer.Blanks(content.acceptedAnswers.map { it.first() })
        is ExerciseContent.OrderLines -> Answer.Order(content.lines.indices.toList())
        is ExerciseContent.PredictOutput -> Answer.Output(content.acceptedOutputs.first())
        is ExerciseContent.FindError -> Answer.ErrorLines(content.errorLines)
        is ExerciseContent.MatchPairs -> Answer.Pairs(content.correctPairs)
    }

    /** Una respuesta completa pero incorrecta para cada tipo de ejercicio. */
    fun wrongAnswerFor(content: ExerciseContent): Answer = when (content) {
        is ExerciseContent.MultipleChoice -> Answer.Choice(setOf(content.options.indices.first { it !in content.correctIndices }))
        is ExerciseContent.TrueFalse -> Answer.TrueFalse(!content.correct)
        is ExerciseContent.FillInBlanks -> Answer.Blanks(List(content.acceptedAnswers.size) { "respuesta_incorrecta" })
        is ExerciseContent.OrderLines -> Answer.Order(content.lines.indices.reversed().toList())
        is ExerciseContent.PredictOutput -> Answer.Output("salida incorrecta")
        is ExerciseContent.FindError -> Answer.ErrorLines(setOf(content.code.lines().size + 1))
        is ExerciseContent.MatchPairs -> Answer.Pairs(content.correctPairs.mapValues { (left, _) -> content.correctPairs.getValue((left + 1) % content.correctPairs.size) })
    }
}
