package com.funcode.academy.testing

import com.funcode.academy.domain.lesson.Answer
import com.funcode.academy.domain.lesson.ExerciseContent
import com.funcode.academy.domain.lesson.Lesson
import com.funcode.academy.domain.lesson.LessonParseResult
import com.funcode.academy.domain.lesson.LessonParser
import java.io.File

/** Contenido real de la app, leído del disco (las pruebas JVM se ejecutan en el directorio del módulo). */
object DemoContent {
    const val LESSON_ID = "java.m02.l01"

    fun lesson(): Lesson {
        val text = File("src/main/assets/content/es/java/lessons/$LESSON_ID.json").readText()
        return (LessonParser.parse(text) as LessonParseResult.Valid).lesson
    }

    fun correctAnswerFor(content: ExerciseContent): Answer = when (content) {
        is ExerciseContent.MultipleChoice -> Answer.Choice(content.correctIndices)
        is ExerciseContent.TrueFalse -> Answer.TrueFalse(content.correct)
        is ExerciseContent.FillInBlanks -> Answer.Blanks(content.acceptedAnswers.map { it.first() })
        is ExerciseContent.OrderLines -> Answer.Order(content.lines.indices.toList())
        is ExerciseContent.PredictOutput -> Answer.Output(content.acceptedOutputs.first())
        is ExerciseContent.FindError -> Answer.ErrorLines(content.errorLines)
        is ExerciseContent.MatchPairs -> Answer.Pairs(content.correctPairs)
    }
}
