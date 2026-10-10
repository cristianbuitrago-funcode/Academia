package com.funcode.academy.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AudienceTest {

    @Test
    fun `en el MVP solo se pueden elegir adolescentes y adultos`() {
        assertEquals(listOf(Audience.TEENS, Audience.ADULTS), Audience.selectable)
    }

    @Test
    fun `la ruta infantil no esta disponible en el MVP`() {
        assertFalse(Audience.KIDS.availableInMvp)
    }
}
