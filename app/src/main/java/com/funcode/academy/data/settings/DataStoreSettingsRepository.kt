package com.funcode.academy.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.funcode.academy.domain.model.Audience
import com.funcode.academy.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Archivo de ajustes: files/datastore/settings.preferences_pb */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Implementación de [SettingsRepository] sobre DataStore (pares clave/valor). */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val audience: Flow<Audience?> = dataStore.data
        .catch { error ->
            // Un archivo dañado no debe cerrar la app: se usan los valores por defecto.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs -> prefs[AUDIENCE_KEY]?.let { name -> Audience.entries.firstOrNull { it.name == name } } }

    override suspend fun setAudience(audience: Audience) {
        require(audience.availableInMvp) { "El público $audience no está disponible en esta versión." }
        dataStore.edit { prefs -> prefs[AUDIENCE_KEY] = audience.name }
    }

    private companion object {
        val AUDIENCE_KEY = stringPreferencesKey("audience")
    }
}
