package com.funcode.academy.domain.runner

import com.funcode.academy.domain.runner.ProgramEvaluation.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgramEvaluationTest {

    private val accepted = listOf("Hola 42")

    @Test
    fun `una salida real que coincide es un intento correcto`() {
        assertEquals(Outcome.OutputMatches, ProgramEvaluation.evaluate(RunResult.Completed("Hola 42\n"), accepted))
        // Mismos criterios que «predecir la salida»: se ignoran espacios finales y saltos de línea finales.
        assertEquals(Outcome.OutputMatches, ProgramEvaluation.evaluate(RunResult.Completed("Hola 42   \r\n\n"), accepted))
    }

    @Test
    fun `una salida real distinta no es correcta aunque el programa se ejecutara`() {
        val outcome = ProgramEvaluation.evaluate(RunResult.Completed("Hola 41"), accepted)
        assertEquals(Outcome.OutputDiffers, outcome)
        assertEquals(false, outcome.attemptCorrect)
    }

    @Test
    fun `un error de ejecucion registra un intento incorrecto`() {
        val r = RunResult.RuntimeError("ArithmeticException", "/ by zero", 3, "")
        assertEquals(false, ProgramEvaluation.evaluate(r, accepted).attemptCorrect)
    }

    @Test
    fun `sin ejecucion terminada no se registra intento (ni penaliza ni premia)`() {
        listOf(
            RunResult.CompilationFailed(listOf(Diagnostic(1, "x"))),
            RunResult.Rejected(RejectReason.EMPTY_SOURCE),
            RunResult.LimitExceeded(Limit.TIME, "Hola 42"),
            RunResult.LimitExceeded(Limit.OUTPUT, "Hola 42"),
            RunResult.Cancelled(),
            RunResult.Unavailable("x"),
        ).forEach { assertNull(it.toString(), ProgramEvaluation.evaluate(it, accepted).attemptCorrect) }
    }

    @Test
    fun `precomprobacion de codigo vacio y demasiado grande`() {
        assertEquals(RejectReason.EMPTY_SOURCE, ProgramEvaluation.precheck(RunRequest("  \n "))?.reason)
        assertEquals(RejectReason.SOURCE_TOO_LARGE, ProgramEvaluation.precheck(RunRequest("x".repeat(10_001)))?.reason)
        assertNull(ProgramEvaluation.precheck(RunRequest("x".repeat(10_000))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `un ejercicio sin salidas aceptadas es un error de contenido`() {
        ProgramEvaluation.evaluate(RunResult.Completed(""), emptyList())
    }
}
