package com.funcode.academy.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.funcode.academy.domain.model.Audience
import com.funcode.academy.domain.settings.LearningPreferences
import com.funcode.academy.domain.settings.SettingsRepository
import com.funcode.academy.domain.settings.TextScale
import com.funcode.academy.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Archivo de ajustes: files/datastore/settings.preferences_pb */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Implementación de [SettingsRepository] sobre DataStore (pares clave/valor). Solo
 * guarda preferencias; el progreso está en Room y no se toca desde aquí.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val preferences: Flow<LearningPreferences> = dataStore.data
        .catch { error ->
            // Un archivo dañado no debe cerrar la app: se usan los valores por defecto.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            LearningPreferences(
                audience = prefs[AUDIENCE_KEY]?.let { name -> Audience.entries.firstOrNull { it.name == name } },
                themeMode = prefs[THEME_KEY]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM,
                textScale = prefs[TEXT_SCALE_KEY]?.let { name -> TextScale.entries.firstOrNull { it.name == name } } ?: TextScale.NORMAL,
                hintsEnabled = prefs[HINTS_KEY] ?: true,
            )
        }

    override val audience: Flow<Audience?> = preferences.map { it.audience }

    override suspend fun setAudience(audience: Audience) {
        require(audience.availableInMvp) { "El público $audience no está disponible en esta versión." }
        dataStore.edit { prefs -> prefs[AUDIENCE_KEY] = audience.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_KEY] = mode.name }
    }

    override suspend fun setTextScale(scale: TextScale) {
        dataStore.edit { it[TEXT_SCALE_KEY] = scale.name }
    }

    override suspend fun setHintsEnabled(enabled: Boolean) {
        dataStore.edit { it[HINTS_KEY] = enabled }
    }

    override suspend fun resetPreferences() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val AUDIENCE_KEY = stringPreferencesKey("audience")
        val THEME_KEY = stringPreferencesKey("theme_mode")
        val TEXT_SCALE_KEY = stringPreferencesKey("text_scale")
        val HINTS_KEY = booleanPreferencesKey("hints_enabled")
    }
}
