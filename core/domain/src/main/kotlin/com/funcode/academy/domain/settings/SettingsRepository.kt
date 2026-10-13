package com.funcode.academy.domain.settings

import com.funcode.academy.domain.model.Audience
import kotlinx.coroutines.flow.Flow

/** Ajustes del estudiante que se conservan entre sesiones (solo en el dispositivo). */
interface SettingsRepository {
    /** Público elegido, o `null` si aún no se ha elegido. */
    val audience: Flow<Audience?>

    /** Todas las preferencias, con sus valores por defecto si no se han cambiado. */
    val preferences: Flow<LearningPreferences>

    /** Guarda el público. Solo admite públicos disponibles en esta versión. */
    suspend fun setAudience(audience: Audience)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setTextScale(scale: TextScale)

    suspend fun setHintsEnabled(enabled: Boolean)

    /** Vuelve a los valores por defecto. No toca el progreso (que está en otra base de datos). */
    suspend fun resetPreferences()
}
