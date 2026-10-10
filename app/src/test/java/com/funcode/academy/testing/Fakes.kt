package com.funcode.academy.testing

import com.funcode.academy.domain.model.Audience
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.progress.XpAward
import com.funcode.academy.domain.progress.XpAwardResult
import com.funcode.academy.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Repositorios en memoria para probar la interfaz sin base de datos. */
class FakeProgressRepository(initialXp: Int = 0) : ProgressRepository {
    private val total = MutableStateFlow(initialXp)
    override fun observeTotalXp(): Flow<Int> = total
    override suspend fun award(award: XpAward): XpAwardResult = error("No se usa en las pruebas de interfaz")
}

class FakeSettingsRepository(initial: Audience? = null) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val audience: Flow<Audience?> = state
    override suspend fun setAudience(audience: Audience) {
        state.value = audience
    }
}
