package com.funcode.academy.domain.rpg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementTest {

    private fun met(state: LearningState) = Achievement.entries.filter { it.isMet(state) }.toSet()

    @Test fun `los ids son unicos y estables`() {
        assertEquals(
            listOf("first_lesson", "three_lessons", "first_assessment_passed", "first_module_mastered", "level_2", "first_challenge"),
            Achievement.entries.map { it.id },
        )
        Achievement.entries.forEach { assertEquals(it, Achievement.fromId(it.id)) }
        assertEquals(null, Achievement.fromId("desconocido"))
    }

    @Test fun `sin actividad no hay logros`() {
        assertTrue(met(LearningState(0, 0, 0, level = 1)).isEmpty())
    }

    @Test fun `cada condicion se cumple solo con su resultado`() {
        assertEquals(setOf(Achievement.FIRST_LESSON), met(LearningState(1, 0, 0, 1)))
        assertEquals(setOf(Achievement.FIRST_LESSON, Achievement.THREE_LESSONS), met(LearningState(3, 0, 0, 1)))
        assertEquals(setOf(Achievement.FIRST_ASSESSMENT_PASSED), met(LearningState(0, 1, 0, 1)))
        assertEquals(setOf(Achievement.LEVEL_2), met(LearningState(0, 0, 0, 2)))
        assertTrue(Achievement.FIRST_MODULE_MASTERED.isMet(LearningState(7, 1, 1, 4)))
        assertEquals(setOf(Achievement.FIRST_CHALLENGE), met(LearningState(0, 0, 0, 1, completedChallenges = 1)))
    }
}
