package com.funcode.academy.domain.lesson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerValidatorTest {

    private fun validate(content: ExerciseContent, answer: Answer) = AnswerValidator.validate(content, answer)
    private fun incomplete(reason: IncompleteReason) = AnswerResult.Incomplete(reason)

    // 1. Selección múltiple

    private val single = ExerciseContent.MultipleChoice(
        listOf(ChoiceOption("A", null), ChoiceOption("B", "B no, porque…"), ChoiceOption("C", null)),
        correctIndices = setOf(0),
    )
    private val multi = single.copy(correctIndices = setOf(0, 2))

    @Test fun `seleccion unica correcta`() = assertEquals(AnswerResult.Correct, validate(single, Answer.Choice(setOf(0))))

    @Test fun `seleccion unica incorrecta devuelve la retroalimentacion de la opcion`() =
        assertEquals(AnswerResult.Incorrect(optionFeedback = "B no, porque…"), validate(single, Answer.Choice(setOf(1))))

    @Test fun `sin seleccion es incompleta`() =
        assertEquals(incomplete(IncompleteReason.NOTHING_SELECTED), validate(single, Answer.Choice(emptySet())))

    @Test fun `seleccion multiple exige todas las correctas`() {
        assertEquals(AnswerResult.Correct, validate(multi, Answer.Choice(setOf(2, 0))))
        assertTrue(validate(multi, Answer.Choice(setOf(0))) is AnswerResult.Incorrect)
        assertTrue(validate(multi, Answer.Choice(setOf(0, 1, 2))) is AnswerResult.Incorrect)
    }

    // 2. Verdadero o falso

    @Test fun `verdadero o falso`() {
        val content = ExerciseContent.TrueFalse("S", correct = false)
        assertEquals(AnswerResult.Correct, validate(content, Answer.TrueFalse(false)))
        assertEquals(AnswerResult.Incorrect(), validate(content, Answer.TrueFalse(true)))
        assertEquals(incomplete(IncompleteReason.NOTHING_SELECTED), validate(content, Answer.TrueFalse(null)))
    }

    // 3. Completar huecos

    private val blanks = ExerciseContent.FillInBlanks(
        "int x = {{0}}; System.out.{{1}}(x);",
        listOf(listOf("x + 1", "1 + x"), listOf("println")),
    )

    @Test fun `huecos correctos ignorando espacios irrelevantes`() {
        assertEquals(AnswerResult.Correct, validate(blanks, Answer.Blanks(listOf("x+1", " println "))))
        assertEquals(AnswerResult.Correct, validate(blanks, Answer.Blanks(listOf("1  +  x", "println"))))
    }

    @Test fun `huecos distinguen mayusculas`() =
        assertEquals(
            AnswerResult.Incorrect(correctParts = 1, totalParts = 2),
            validate(blanks, Answer.Blanks(listOf("x + 1", "PrintLn"))),
        )

    @Test fun `huecos vacios son incompletos`() {
        assertEquals(incomplete(IncompleteReason.EMPTY_BLANKS), validate(blanks, Answer.Blanks(listOf("x+1", "  "))))
        assertEquals(incomplete(IncompleteReason.EMPTY_BLANKS), validate(blanks, Answer.Blanks(listOf("x+1"))))
    }

    @Test fun `los espacios dentro de un texto entre comillas si cuentan`() {
        val content = ExerciseContent.FillInBlanks("{{0}}", listOf(listOf("\"Hola mundo\"")))
        assertEquals(AnswerResult.Correct, validate(content, Answer.Blanks(listOf(" \"Hola mundo\" "))))
        assertTrue(validate(content, Answer.Blanks(listOf("\"Hola  mundo\""))) is AnswerResult.Incorrect)
    }

    // 4. Ordenar líneas

    private val order = ExerciseContent.OrderLines(
        listOf("class A {", "void m() {", "a();", "b();", "}", "}"),
        alternativeOrders = emptyList(),
    )

    @Test fun `orden correcto`() = assertEquals(AnswerResult.Correct, validate(order, Answer.Order(listOf(0, 1, 2, 3, 4, 5))))

    @Test fun `lineas identicas son intercambiables`() =
        assertEquals(AnswerResult.Correct, validate(order, Answer.Order(listOf(0, 1, 2, 3, 5, 4))))

    @Test fun `orden incorrecto`() = assertEquals(AnswerResult.Incorrect(), validate(order, Answer.Order(listOf(0, 1, 3, 2, 4, 5))))

    @Test fun `orden con lineas repetidas o ausentes no es valido`() {
        assertEquals(incomplete(IncompleteReason.INVALID_ORDER), validate(order, Answer.Order(listOf(0, 0, 1, 2, 3, 4))))
        assertEquals(incomplete(IncompleteReason.INVALID_ORDER), validate(order, Answer.Order(listOf(0, 1))))
    }

    @Test fun `orden alternativo declarado es valido`() {
        val content = ExerciseContent.OrderLines(listOf("a();", "b();"), alternativeOrders = listOf(listOf(1, 0)))
        assertEquals(AnswerResult.Correct, validate(content, Answer.Order(listOf(1, 0))))
    }

    // 5. Predecir la salida

    private val output = ExerciseContent.PredictOutput("…", listOf("Hola\nMundo"))

    @Test fun `salida correcta con saltos de linea de Windows y espacios finales`() =
        assertEquals(AnswerResult.Correct, validate(output, Answer.Output("Hola  \r\nMundo\n\n")))

    @Test fun `la salida distingue mayusculas y espacios internos`() {
        assertTrue(validate(output, Answer.Output("hola\nMundo")) is AnswerResult.Incorrect)
        assertTrue(validate(output, Answer.Output("Hola Mundo")) is AnswerResult.Incorrect)
        assertTrue(validate(output, Answer.Output(" Hola\nMundo")) is AnswerResult.Incorrect)
    }

    @Test fun `salida vacia es incompleta`() =
        assertEquals(incomplete(IncompleteReason.EMPTY_OUTPUT), validate(output, Answer.Output(" \n ")))

    // 6. Encontrar errores

    private val findError = ExerciseContent.FindError("a\nb\nc\nd", errorLines = setOf(2, 4))

    @Test fun `errores encontrados`() = assertEquals(AnswerResult.Correct, validate(findError, Answer.ErrorLines(setOf(4, 2))))

    @Test fun `errores parciales indican cuantos acerto`() =
        assertEquals(AnswerResult.Incorrect(correctParts = 1, totalParts = 2), validate(findError, Answer.ErrorLines(setOf(2))))

    @Test fun `marcar lineas de mas es incorrecto`() =
        assertTrue(validate(findError, Answer.ErrorLines(setOf(1, 2, 4))) is AnswerResult.Incorrect)

    @Test fun `sin lineas marcadas es incompleto`() =
        assertEquals(incomplete(IncompleteReason.NOTHING_SELECTED), validate(findError, Answer.ErrorLines(emptySet())))

    // 7. Relacionar conceptos

    private val match = ExerciseContent.MatchPairs(listOf("a", "b", "c"), listOf("x", "y", "z"), mapOf(0 to 2, 1 to 0, 2 to 1))

    @Test fun `parejas correctas`() = assertEquals(AnswerResult.Correct, validate(match, Answer.Pairs(mapOf(0 to 2, 1 to 0, 2 to 1))))

    @Test fun `parejas parcialmente correctas`() =
        assertEquals(
            AnswerResult.Incorrect(correctParts = 1, totalParts = 3),
            validate(match, Answer.Pairs(mapOf(0 to 2, 1 to 1, 2 to 0))),
        )

    @Test fun `parejas incompletas o repetidas`() {
        assertEquals(incomplete(IncompleteReason.NOT_ALL_PAIRS), validate(match, Answer.Pairs(mapOf(0 to 2))))
        assertEquals(incomplete(IncompleteReason.DUPLICATED_PAIR), validate(match, Answer.Pairs(mapOf(0 to 2, 1 to 2, 2 to 1))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `un tipo de respuesta que no corresponde al ejercicio es un error de programacion`() {
        validate(match, Answer.TrueFalse(true))
    }
}
