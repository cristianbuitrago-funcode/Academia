package com.funcode.academy.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudienceTest {

    @Test
    fun `desde la Fase 7 se pueden elegir los tres publicos`() {
        // Decisión del responsable (Fase 7): se habilita la presentación infantil (cambia D3).
        assertEquals(listOf(Audience.KIDS, Audience.TEENS, Audience.ADULTS), Audience.selectable)
        assertTrue(Audience.entries.all { it.availableInMvp })
    }
}
