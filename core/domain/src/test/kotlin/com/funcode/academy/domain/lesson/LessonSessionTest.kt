package com.funcode.academy.domain.lesson

import com.funcode.academy.domain.lesson.TestLessons.exercise
import com.funcode.academy.domain.lesson.TestLessons.lesson
import com.funcode.academy.domain.lesson.TestLessons.trueFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonSessionTest {

    private val session = LessonSession(lesson(exercise("e1", trueFalse), exercise("e2", trueFalse), exercise("e3", trueFalse)))

    @Test fun `las pistas se revelan de una en una y no pasan del total`() {
        val s = session.revealHint().revealHint().revealHint()
        assertEquals(2, s.currentProgress.hintsRevealed)
    }

    @Test fun `una respuesta incompleta no cuenta como intento`() {
        val r = session.submit(Answer.TrueFalse(null))
        assertEquals(0, r.session.currentProgress.attempts)
        assertTrue(r.result is AnswerResult.Incomplete)
    }

    @Test fun `un fallo y un acierto suman dos intentos`() {
        val afterWrong = session.submit(Answer.TrueFalse(false)).session
        val afterRight = afterWrong.submit(Answer.TrueFalse(true))
        assertEquals(AnswerResult.Correct, afterRight.result)
        assertEquals(2, afterRight.session.currentProgress.attempts)
        assertTrue(afterRight.session.currentProgress.solved)
    }

    @Test(expected = IllegalStateException::class)
    fun `un ejercicio resuelto no admite mas intentos`() {
        session.submit(Answer.TrueFalse(true)).session.submit(Answer.TrueFalse(true))
    }

    @Test fun `no se pueden pedir pistas de un ejercicio ya resuelto`() {
        val solved = session.submit(Answer.TrueFalse(true)).session
        assertEquals(0, solved.revealHint().currentProgress.hintsRevealed)
    }

    @Test fun `next avanza y termina despues del ultimo ejercicio`() {
        val s = session.next().next()
        assertEquals(2, s.currentIndex)
        assertFalse(s.finished)
        assertTrue(s.next().finished)
    }

    @Test fun `el resumen cuenta aciertos al primer intento, tras reintentos, sin resolver y pistas`() {
        var s = session.submit(Answer.TrueFalse(true)).session.next() // e1: primer intento
        s = s.revealHint().submit(Answer.TrueFalse(false)).session // e2: fallo con 1 pista
        s = s.submit(Answer.TrueFalse(true)).session.next() // e2: acierto en el segundo intento
        s = s.submit(Answer.TrueFalse(false)).session.next() // e3: sin resolver
        val summary = s.summary()
        assertTrue(s.finished)
        assertEquals(3, summary.total)
        assertEquals(2, summary.solved)
        assertEquals(1, summary.solvedFirstTry)
        assertEquals(1, summary.unsolved)
        assertEquals(1, summary.hintsUsed)
        assertEquals(4, summary.attempts)
        assertNull(summary.assessmentPassed)
    }

    @Test fun `la evaluacion se aprueba segun el umbral de aciertos al primer intento`() {
        val withAssessment = LessonSession(
            lesson(exercise("e1", trueFalse), exercise("e2", trueFalse), assessment = Assessment(listOf("e1", "e2"), 0.5)),
        )
        val passed = withAssessment.submit(Answer.TrueFalse(true)).session.next().submit(Answer.TrueFalse(false)).session
        assertEquals(true, passed.summary().assessmentPassed)
        val failed = withAssessment.submit(Answer.TrueFalse(false)).session
        assertEquals(false, failed.summary().assessmentPassed)
    }
}
