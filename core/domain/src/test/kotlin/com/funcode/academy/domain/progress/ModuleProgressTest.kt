package com.funcode.academy.domain.progress

import com.funcode.academy.domain.lesson.CourseModule
import com.funcode.academy.domain.lesson.LessonKind
import com.funcode.academy.domain.lesson.LessonSummary
import com.funcode.academy.domain.lesson.ReviewStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModuleProgressTest {

    private fun summary(id: String, kind: LessonKind = LessonKind.LESSON, vararg exercises: String) =
        LessonSummary(id, "m", id, "d", ReviewStatus.DRAFT, kind, exercises.toList())

    private val l1 = summary("m.l1", LessonKind.LESSON, "m.l1.e1", "m.l1.e2")
    private val l2 = summary("m.l2", LessonKind.LESSON, "m.l2.e1")
    private val eval = summary("m.eval", LessonKind.ASSESSMENT, "m.eval.q1")
    private val module = CourseModule("m", 1, "M", "D", ReviewStatus.DRAFT, listOf(l1, l2), eval)

    @Test fun `sin progreso, la siguiente actividad es la primera leccion`() {
        val p = ModuleProgress.of(module, emptySet(), emptyMap())
        assertEquals(NextActivity.Lesson(l1), p.next)
        assertEquals(0, p.completedLessons)
        assertNull(p.bestAssessmentScore)
    }

    @Test fun `una leccion a medias no cuenta como completada`() {
        val p = ModuleProgress.of(module, setOf("m.l1.e1"), emptyMap())
        assertEquals(1, p.lessons[0].solvedExercises)
        assertEquals(false, p.lessons[0].completed)
        assertEquals(NextActivity.Lesson(l1), p.next)
    }

    @Test fun `con todas las lecciones completadas toca la evaluacion`() {
        val p = ModuleProgress.of(module, setOf("m.l1.e1", "m.l1.e2", "m.l2.e1"), emptyMap())
        assertEquals(2, p.completedLessons)
        assertEquals(NextActivity.Assessment(eval), p.next)
    }

    @Test fun `con la evaluacion hecha el modulo esta terminado`() {
        val p = ModuleProgress.of(module, setOf("m.l1.e1", "m.l1.e2", "m.l2.e1"), mapOf("m" to 0.7))
        assertEquals(NextActivity.ModuleDone, p.next)
        assertEquals(0.7, p.bestAssessmentScore!!, 0.0)
    }

    @Test fun `resolver otra vez los mismos ejercicios no duplica nada`() {
        val solved = setOf("m.l1.e1", "m.l1.e2", "m.l1.e1")
        assertEquals(1, ModuleProgress.of(module, solved, emptyMap()).completedLessons)
    }

    private val reto = summary("m.reto", LessonKind.CHALLENGE, "m.reto.s1", "m.reto.s2")
    private val withChallenge = module.copy(challenge = reto)
    private val allLessons = setOf("m.l1.e1", "m.l1.e2", "m.l2.e1")

    @Test fun `el reto esta cerrado hasta completar todas las lecciones`() {
        val p = ModuleProgress.of(withChallenge, setOf("m.l1.e1", "m.l1.e2"), emptyMap()).challenge!!
        assertEquals(false, p.unlocked)
        assertEquals(1, p.lessonsRemaining)
        assertEquals(true, ModuleProgress.of(withChallenge, allLessons, emptyMap()).challenge!!.unlocked)
    }

    @Test fun `tras la evaluacion toca el reto y al superarlo el modulo esta terminado`() {
        val evaluated = ModuleProgress.of(withChallenge, allLessons + "m.reto.s1", mapOf("m" to 0.9))
        assertEquals(NextActivity.Challenge(reto), evaluated.next)
        assertEquals(1, evaluated.challenge!!.solvedSteps)
        assertEquals(false, evaluated.challenge!!.completed)
        val done = ModuleProgress.of(withChallenge, allLessons + setOf("m.reto.s1", "m.reto.s2"), mapOf("m" to 0.9))
        assertEquals(true, done.challenge!!.completed)
        assertEquals(NextActivity.ModuleDone, done.next)
    }

    @Test fun `un modulo sin reto no tiene estado de reto`() {
        assertNull(ModuleProgress.of(module, allLessons, emptyMap()).challenge)
    }
}
