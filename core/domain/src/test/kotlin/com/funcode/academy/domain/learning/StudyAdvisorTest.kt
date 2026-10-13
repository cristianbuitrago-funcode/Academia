package com.funcode.academy.domain.learning

import com.funcode.academy.domain.lesson.CourseModule
import com.funcode.academy.domain.lesson.LessonKind
import com.funcode.academy.domain.lesson.LessonSummary
import com.funcode.academy.domain.lesson.ReviewStatus
import com.funcode.academy.domain.progress.ExerciseAttempt
import com.funcode.academy.domain.progress.ExerciseHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyAdvisorTest {

    private fun lesson(id: String, n: Int, kind: LessonKind = LessonKind.LESSON) =
        LessonSummary(id, "m", id, "d", ReviewStatus.DRAFT, kind, (1..n).map { "$id.e$it" })

    private val l1 = lesson("m.l1", 3)
    private val l2 = lesson("m.l2", 2)
    private val eval = lesson("m.eval", 2, LessonKind.ASSESSMENT)
    private val module = CourseModule("m", 1, "M", "D", ReviewStatus.DRAFT, listOf(l1, l2), eval)

    private fun recommend(attempts: List<ExerciseAttempt>, scores: Map<String, Double> = emptyMap()) =
        StudyAdvisor.recommend(listOf(module), ExerciseHistory.fromAttempts(attempts), scores, mapOf("m.eval" to 0.8))

    private fun ok(id: String) = ExerciseAttempt(id, correct = true, hintsUsed = 0)
    private fun fail(id: String) = ExerciseAttempt(id, correct = false, hintsUsed = 0)
    private fun hinted(id: String) = ExerciseAttempt(id, correct = true, hintsUsed = 1)

    @Test fun `sin intentos no hay recomendaciones`() = assertTrue(recommend(emptyList()).isEmpty())

    @Test fun `una leccion empezada se recomienda retomar`() {
        assertEquals(listOf(Recommendation.ResumeLesson(l1, solved = 1, total = 3)), recommend(listOf(ok("m.l1.e1"))))
        // Solo un intento fallido también cuenta como empezada.
        assertEquals(listOf(Recommendation.ResumeLesson(l2, 0, 2)), recommend(listOf(fail("m.l2.e1"))))
    }

    @Test fun `una leccion completada sin dificultades no se recomienda`() =
        assertTrue(recommend(listOf(ok("m.l1.e1"), ok("m.l1.e2"), ok("m.l1.e3"))).isEmpty())

    @Test fun `con dos o mas ejercicios que costaron se recomienda repasar`() {
        val one = listOf(fail("m.l1.e1"), ok("m.l1.e1"), ok("m.l1.e2"), ok("m.l1.e3"))
        assertTrue(recommend(one).isEmpty())
        val two = one + listOf(hinted("m.l2.e1"), fail("m.l2.e2"), ok("m.l2.e2"))
        assertEquals(listOf(Recommendation.ReviewLesson(l2, struggledExercises = 2)), recommend(two))
    }

    @Test fun `evaluacion no aprobada sugiere repasar las lecciones marcadas`() {
        val attempts = listOf(ok("m.l1.e1"), ok("m.l1.e2"), ok("m.l1.e3"), hinted("m.l2.e1"), hinted("m.l2.e2"))
        val r = recommend(attempts, mapOf("m" to 0.5))
        assertEquals(Recommendation.RetryAssessment(eval, 50, 80, listOf(l2)), r.last())
        // Aprobada: no se recomienda repetirla.
        assertTrue(recommend(attempts, mapOf("m" to 0.8)).none { it is Recommendation.RetryAssessment })
    }

    @Test fun `orden retomar, repasar, evaluacion`() {
        val attempts = listOf(hinted("m.l2.e1"), hinted("m.l2.e2"), ok("m.l1.e1"))
        val r = recommend(attempts, mapOf("m" to 0.0))
        assertEquals(
            listOf(Recommendation.ResumeLesson::class, Recommendation.ReviewLesson::class, Recommendation.RetryAssessment::class),
            r.map { it::class },
        )
    }
}
