package com.funcode.academy.ui.profile

import com.funcode.academy.domain.progress.XpEntry
import com.funcode.academy.domain.progress.XpSource
import com.funcode.academy.domain.progress.XpSourceType
import com.funcode.academy.domain.rpg.Achievement
import com.funcode.academy.ui.screens.profile.ProfileViewModel
import com.funcode.academy.ui.screens.profile.RewardTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** El perfil se calcula solo a partir del registro de XP y de los logros guardados (JVM). */
class ProfileStateTest {

    private fun entry(type: XpSourceType, id: String, amount: Int) = XpEntry(XpSource(type, id), amount, 0)

    @Test
    fun sinActividadTodoEstaVacioYEnElNivel1() {
        val state = ProfileViewModel.profileStateOf(emptyList(), emptyMap())
        assertEquals(1, state.level.level)
        assertEquals(0, state.level.totalXp)
        assertEquals(emptyMap<XpSourceType, RewardTotals>(), state.rewards)
        assertEquals(0, state.unlockedCount)
        assertEquals(Achievement.entries, state.achievements.map { it.achievement })
    }

    @Test
    fun agrupaLasRecompensasPorTipoYCalculaElNivel() {
        val entries = listOf(
            entry(XpSourceType.EXERCISE, "e1", 10),
            entry(XpSourceType.EXERCISE, "e2", 15),
            entry(XpSourceType.LESSON, "l1", 20),
            entry(XpSourceType.ASSESSMENT, "eval", 100),
        )
        val state = ProfileViewModel.profileStateOf(entries, mapOf("first_lesson" to 42L, "desconocido" to 1L))
        assertEquals(145, state.level.totalXp)
        assertEquals(2, state.level.level)
        assertEquals(RewardTotals(2, 25), state.rewards[XpSourceType.EXERCISE])
        assertEquals(RewardTotals(1, 20), state.rewards[XpSourceType.LESSON])
        assertEquals(RewardTotals(1, 100), state.rewards[XpSourceType.ASSESSMENT])
        // Un id desconocido (p. ej. de otra versión) no aparece ni cuenta.
        assertEquals(1, state.unlockedCount)
        assertEquals(42L, state.achievements.single { it.achievement == Achievement.FIRST_LESSON }.unlockedAt)
        assertNull(state.achievements.single { it.achievement == Achievement.LEVEL_2 }.unlockedAt)
    }
}
