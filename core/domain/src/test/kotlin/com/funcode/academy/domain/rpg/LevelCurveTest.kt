package com.funcode.academy.domain.rpg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LevelCurveTest {

    @Test fun `xp acumulado para empezar cada nivel`() {
        assertEquals(listOf(0, 100, 300, 600, 1000, 1500), (1..6).map { LevelCurve.xpToReach(it) })
    }

    @Test fun `estado inicial sin XP`() {
        val p = LevelCurve.progressFor(0)
        assertEquals(LevelProgress(level = 1, totalXp = 0, xpIntoLevel = 0, xpForNextLevel = 100), p)
        assertEquals(100, p.xpRemaining)
        assertEquals(0f, p.fraction)
    }

    @Test fun `limites exactos entre niveles`() {
        assertEquals(1, LevelCurve.progressFor(99).level)
        assertEquals(2, LevelCurve.progressFor(100).level)
        assertEquals(2, LevelCurve.progressFor(299).level)
        assertEquals(3, LevelCurve.progressFor(300).level)
        assertEquals(5, LevelCurve.progressFor(1000).level)
    }

    @Test fun `progreso dentro del nivel`() {
        val p = LevelCurve.progressFor(450) // nivel 3: de 300 a 600
        assertEquals(3, p.level)
        assertEquals(150, p.xpIntoLevel)
        assertEquals(300, p.xpForNextLevel)
        assertEquals(150, p.xpRemaining)
        assertEquals(0.5f, p.fraction)
    }

    @Test fun `es determinista`() {
        assertEquals(LevelCurve.progressFor(910), LevelCurve.progressFor(910))
    }

    @Test fun `rechaza valores no validos`() {
        assertThrows(IllegalArgumentException::class.java) { LevelCurve.progressFor(-1) }
        assertThrows(IllegalArgumentException::class.java) { LevelCurve.xpToReach(0) }
    }
}
