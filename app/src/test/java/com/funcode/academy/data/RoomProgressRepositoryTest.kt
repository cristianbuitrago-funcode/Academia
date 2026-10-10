package com.funcode.academy.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.funcode.academy.data.local.FuncodeDatabase
import com.funcode.academy.data.local.XpLedgerEntity
import com.funcode.academy.data.progress.RoomProgressRepository
import com.funcode.academy.domain.progress.XpAward
import com.funcode.academy.domain.progress.XpAwardResult
import com.funcode.academy.domain.progress.XpSource
import com.funcode.academy.domain.progress.XpSourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Pruebas de la base de datos real (Room) ejecutadas en la JVM con Robolectric. */
@RunWith(AndroidJUnit4::class)
class RoomProgressRepositoryTest {

    private lateinit var database: FuncodeDatabase
    private lateinit var repository: RoomProgressRepository

    private val exercise = XpSource(XpSourceType.EXERCISE, "java.m01.l01.e01")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FuncodeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomProgressRepository(database.xpLedgerDao(), clock = { 1_000L })
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sinActividadesElXpTotalEsCero() = runTest {
        assertEquals(0, repository.observeTotalXp().first())
    }

    @Test
    fun laMismaActividadSoloConcedeXpUnaVez() = runTest {
        val first = repository.award(XpAward(exercise, 10))
        val second = repository.award(XpAward(exercise, 10))

        assertEquals(XpAwardResult.Granted(10), first)
        assertEquals(XpAwardResult.AlreadyGranted, second)
        assertEquals(10, repository.observeTotalXp().first())
    }

    @Test
    fun repetirConOtraCantidadTampocoSumaXp() = runTest {
        repository.award(XpAward(exercise, 10))
        repository.award(XpAward(exercise, 50))

        assertEquals(10, repository.observeTotalXp().first())
    }

    @Test
    fun actividadesDistintasSuman() = runTest {
        repository.award(XpAward(exercise, 10))
        repository.award(XpAward(XpSource(XpSourceType.EXERCISE, "java.m01.l01.e02"), 15))
        repository.award(XpAward(XpSource(XpSourceType.LESSON, "java.m01.l01"), 20))

        assertEquals(45, repository.observeTotalXp().first())
    }

    @Test
    fun laRestriccionUnicaEstaEnLaBaseDeDatosNoSoloEnElRepositorio() = runTest {
        // Se inserta directamente con el DAO, sin pasar por el repositorio.
        val dao = database.xpLedgerDao()
        val entry = XpLedgerEntity(sourceType = "EXERCISE", sourceId = "x", amount = 5, timestamp = 0)

        assertEquals(true, dao.insert(entry) > 0)
        assertEquals(-1L, dao.insert(entry.copy(amount = 99)))
        assertEquals(5, dao.observeTotalXp().first())
    }
}
