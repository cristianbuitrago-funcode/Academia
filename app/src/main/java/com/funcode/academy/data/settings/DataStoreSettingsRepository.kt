package com.funcode.academy.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.funcode.academy.domain.model.Audience
import com.funcode.academy.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import java.io.File
import java.io.IOException

/**
 * Crea el DataStore de ajustes sobre [file]. Debe existir una sola instancia por
 * archivo (en la app la crea `AppContainer`).
 *
 * Usa el almacenamiento basado en Okio, que reemplaza el archivo de forma atómica
 * también en la JVM de Windows (donde se ejecutan las pruebas); el almacenamiento
 * por defecto falla allí al sobrescribir un archivo existente.
 */
fun createSettingsDataStore(file: File): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { file.absolutePath.toPath() })

/** Archivo de ajustes: files/datastore/settings.preferences_pb */
fun Context.settingsDataStoreFile(): File = File(filesDir, "datastore/settings.preferences_pb")

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
