package com.funcode.academy.domain.rpg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RewardRulesTest {

    @Test fun `el XP base crece 5 por nivel de dificultad`() {
        assertEquals(listOf(10, 15, 20, 25, 30), (1..5).map { RewardRules.exerciseBaseXp(it) })
    }

    @Test fun `cada pista resta un 25 por ciento redondeando hacia abajo`() {
        assertEquals(10, RewardRules.exerciseXp(difficulty = 1, hintsUsed = 0))
        assertEquals(7, RewardRules.exerciseXp(difficulty = 1, hintsUsed = 1))
        assertEquals(11, RewardRules.exerciseXp(difficulty = 2, hintsUsed = 1))
        assertEquals(15, RewardRules.exerciseXp(difficulty = 3, hintsUsed = 1))
        assertEquals(18, RewardRules.exerciseXp(difficulty = 4, hintsUsed = 1))
    }

    @Test fun `la penalizacion nunca baja del 50 por ciento`() {
        assertEquals(10, RewardRules.exerciseXp(difficulty = 3, hintsUsed = 2))
        assertEquals(10, RewardRules.exerciseXp(difficulty = 3, hintsUsed = 3))
        assertEquals(10, RewardRules.exerciseXp(difficulty = 3, hintsUsed = 50))
        assertEquals(5, RewardRules.exerciseXp(difficulty = 1, hintsUsed = 9))
    }

    @Test fun `rechaza valores no validos`() {
        assertThrows(IllegalArgumentException::class.java) { RewardRules.exerciseBaseXp(0) }
        assertThrows(IllegalArgumentException::class.java) { RewardRules.exerciseBaseXp(6) }
        assertThrows(IllegalArgumentException::class.java) { RewardRules.exerciseXp(2, -1) }
    }
}
