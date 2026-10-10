package com.funcode.academy.domain.settings

import com.funcode.academy.domain.model.Audience
import kotlinx.coroutines.flow.Flow

/** Ajustes del estudiante que se conservan entre sesiones. */
interface SettingsRepository {
    /** Público elegido, o `null` si aún no se ha elegido. */
    val audience: Flow<Audience?>

    /** Guarda el público. Solo admite públicos disponibles en esta versión. */
    suspend fun setAudience(audience: Audience)
}
