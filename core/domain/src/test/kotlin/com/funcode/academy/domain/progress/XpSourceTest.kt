package com.funcode.academy.domain.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class XpSourceTest {

    @Test(expected = IllegalArgumentException::class)
    fun `una actividad sin identificador se rechaza`() {
        XpSource(XpSourceType.EXERCISE, "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no se puede conceder XP cero`() {
        XpAward(XpSource(XpSourceType.EXERCISE, "java.m01.l01.e01"), 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no se puede conceder XP negativo`() {
        XpAward(XpSource(XpSourceType.EXERCISE, "java.m01.l01.e01"), -10)
    }

    @Test
    fun `dos fuentes con el mismo tipo e id son la misma actividad`() {
        assertEquals(
            XpSource(XpSourceType.LESSON, "java.m01.l01"),
            XpSource(XpSourceType.LESSON, "java.m01.l01"),
        )
    }

    @Test
    fun `el mismo id con distinto tipo es otra actividad`() {
        assertNotEquals(
            XpSource(XpSourceType.LESSON, "java.m01"),
            XpSource(XpSourceType.ASSESSMENT, "java.m01"),
        )
    }
}
