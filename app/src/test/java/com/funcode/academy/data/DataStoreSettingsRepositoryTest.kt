package com.funcode.academy.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.funcode.academy.data.settings.DataStoreSettingsRepository
import com.funcode.academy.domain.model.Audience
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Pruebas de DataStore sobre un archivo real en una carpeta temporal (sin Android). */
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun file(): File = File(tempFolder.root, "settings.preferences_pb")

    /** Abre el archivo, ejecuta [block] y cierra el DataStore, como si la app se cerrara. */
    private fun <T> withRepository(block: suspend (DataStoreSettingsRepository) -> T): T = runBlocking {
        val job = Job()
        // Mismo almacenamiento que la app (createSettingsDataStore), con un ámbito
        // propio para poder cerrarlo y simular que la app se cierra.
        val dataStore = PreferenceDataStoreFactory.createWithPath(
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { file().absolutePath.toPath() },
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

    @Test(expected = IllegalArgumentException::class)
    fun laRutaInfantilNoSePuedeElegirEnElMvp() {
        withRepository { it.setAudience(Audience.KIDS) }
    }
}
