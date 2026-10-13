package com.funcode.academy.domain.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseHistoryTest {

    private fun a(id: String, correct: Boolean, hints: Int = 0) = ExerciseAttempt(id, correct, hints)

    @Test fun `historial por ejercicio a partir de los intentos en orden`() {
        val h = ExerciseHistory.fromAttempts(
            listOf(
                a("limpio", true),
                a("fallo", false), a("fallo", true),
                a("pista", true, hints = 1), a("pista", true),
                a("pendiente", false), a("pendiente", false),
            ),
        )
        assertEquals(ExerciseHistory("limpio", 1, failedBeforeFirstCorrect = false, hintsAtFirstCorrect = 0), h["limpio"])
        assertFalse(h.getValue("limpio").struggled)
        assertTrue(h.getValue("fallo").struggled)
        assertTrue(h.getValue("pista").struggled) // el primer acierto usó una pista
        assertFalse(h.getValue("pendiente").solved)
        assertFalse(h.getValue("pendiente").struggled)
        assertEquals(2, h.getValue("pendiente").attempts)
    }

    @Test fun `sin intentos no hay historial`() = assertTrue(ExerciseHistory.fromAttempts(emptyList()).isEmpty())
}
