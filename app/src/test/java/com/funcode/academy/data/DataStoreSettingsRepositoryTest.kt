package com.funcode.academy.data

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import com.funcode.academy.data.settings.DataStoreSettingsRepository
import com.funcode.academy.domain.model.Audience
import com.funcode.academy.domain.settings.LearningPreferences
import com.funcode.academy.domain.settings.TextScale
import com.funcode.academy.domain.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Pruebas de DataStore sobre un archivo real en una carpeta temporal (sin Android).
 * Comprueban la lógica del repositorio y que los datos persisten al reabrir.
 */
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun file(): File = File(tempFolder.root, "settings.preferences_pb")

    /** Abre el archivo, ejecuta [block] y cierra el DataStore, como si la app se cerrara. */
    private fun <T> withRepository(block: suspend (DataStoreSettingsRepository) -> T): T = runBlocking {
        val job = Job()
        // Almacenamiento basado en Okio: en la JVM de Windows el almacenamiento por
        // defecto (FileStorage, File.renameTo) no puede reemplazar un archivo existente.
        // En Android la app usa FileStorage, donde renombrar sí reemplaza el archivo.
        val dataStore = PreferenceDataStoreFactory.create(
            storage = OkioStorage(
                fileSystem = FileSystem.SYSTEM,
                serializer = PreferencesSerializer,
                producePath = { file().absolutePath.toPath() },
            ),
            scope = CoroutineScope(Dispatchers.IO + job),
        )
        try {
            block(DataStoreSettingsRepository(dataStore))
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun alPrincipioNoHayPublicoElegido() {
        assertNull(withRepository { it.audience.first() })
    }

    @Test
    fun elPublicoElegidoSeConservaAlReabrir() {
        withRepository { it.setAudience(Audience.ADULTS) }

        assertEquals(Audience.ADULTS, withRepository { it.audience.first() })
    }

    @Test
    fun cambiarDePublicoReemplazaElAnterior() {
        withRepository { it.setAudience(Audience.ADULTS) }
        withRepository { it.setAudience(Audience.TEENS) }

        assertEquals(Audience.TEENS, withRepository { it.audience.first() })
    }

    @Test
    fun laPresentacionInfantilSePuedeElegirDesdeLaFase7() {
        withRepository { it.setAudience(Audience.KIDS) }
        assertEquals(Audience.KIDS, withRepository { it.audience.first() })
    }

    @Test
    fun preferenciasPorDefecto() {
        assertEquals(LearningPreferences(), withRepository { it.preferences.first() })
    }

    @Test
    fun cadaPreferenciaSeConservaAlReabrir() {
        withRepository {
            it.setThemeMode(ThemeMode.DARK)
            it.setTextScale(TextScale.EXTRA_LARGE)
            it.setHintsEnabled(false)
            it.setAudience(Audience.TEENS)
        }
        assertEquals(
            LearningPreferences(Audience.TEENS, ThemeMode.DARK, TextScale.EXTRA_LARGE, hintsEnabled = false),
            withRepository { it.preferences.first() },
        )
    }

    @Test
    fun restablecerVuelveALosValoresPorDefecto() {
        withRepository {
            it.setThemeMode(ThemeMode.LIGHT)
            it.setHintsEnabled(false)
            it.setAudience(Audience.KIDS)
        }
        withRepository { it.resetPreferences() }
        assertEquals(LearningPreferences(), withRepository { it.preferences.first() })
    }
}
