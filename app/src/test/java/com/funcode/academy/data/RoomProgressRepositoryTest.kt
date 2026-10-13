package com.funcode.academy.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.funcode.academy.data.local.FuncodeDatabase
import com.funcode.academy.data.local.ModuleProgressDao
import com.funcode.academy.data.local.XpLedgerEntity
import com.funcode.academy.data.progress.RoomProgressRepository
import com.funcode.academy.domain.lesson.AssessmentScore
import com.funcode.academy.domain.progress.ExerciseAttempt
import com.funcode.academy.domain.progress.ExerciseHistory
import com.funcode.academy.domain.progress.XpAward
import com.funcode.academy.domain.progress.XpEntry
import com.funcode.academy.domain.progress.XpAwardResult
import com.funcode.academy.domain.progress.XpSource
import com.funcode.academy.domain.progress.XpSourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        repository = repositoryFor(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun repositoryFor(db: FuncodeDatabase) =
        RoomProgressRepository(db.xpLedgerDao(), db.exerciseAttemptDao(), db.moduleProgressDao(), db.achievementDao(), clock = { 1_000L })

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

    @Test
    fun losIntentosSeGuardanSinElTextoDeLaRespuesta() = runTest {
        repository.recordAttempt(ExerciseAttempt("java.m01.l05.e01", correct = false, hintsUsed = 1))
        repository.recordAttempt(ExerciseAttempt("java.m01.l05.e01", correct = true, hintsUsed = 1))
        repository.recordAttempt(ExerciseAttempt("java.m01.l05.e02", correct = true, hintsUsed = 0))

        val attempts = database.exerciseAttemptDao().attemptsFor("java.m01.l05.e01")
        assertEquals(listOf(false, true), attempts.map { it.correct })
        assertEquals(listOf(1, 1), attempts.map { it.hintsUsed })
        assertEquals(1, database.exerciseAttemptDao().attemptsFor("java.m01.l05.e02").size)
    }

    @Test
    fun losEjerciciosResueltosSeDerivanDeLosIntentosSinDuplicarse() = runTest {
        repository.recordAttempt(ExerciseAttempt("java.m01.l01.e01", correct = false, hintsUsed = 0))
        assertEquals(emptySet<String>(), repository.observeSolvedExerciseIds().first())

        repository.recordAttempt(ExerciseAttempt("java.m01.l01.e01", correct = true, hintsUsed = 0))
        repository.recordAttempt(ExerciseAttempt("java.m01.l01.e01", correct = true, hintsUsed = 0))
        repository.recordAttempt(ExerciseAttempt("java.m01.l01.e02", correct = true, hintsUsed = 1))

        assertEquals(setOf("java.m01.l01.e01", "java.m01.l01.e02"), repository.observeSolvedExerciseIds().first())
    }

    @Test
    fun sinEvaluacionesNoHayResultados() = runTest {
        assertEquals(emptyMap<String, Double>(), repository.observeBestAssessmentScores().first())
    }

    @Test
    fun laEvaluacionConservaSoloElMejorResultadoEnUnaFila() = runTest {
        repository.recordAssessmentResult("java.m01", AssessmentScore(correct = 6, total = 10))
        repository.recordAssessmentResult("java.m01", AssessmentScore(correct = 9, total = 10))
        repository.recordAssessmentResult("java.m01", AssessmentScore(correct = 4, total = 10))

        assertEquals(mapOf("java.m01" to 0.9), repository.observeBestAssessmentScores().first())
        val rows = database.moduleProgressDao().observeAll().first()
        assertEquals(1, rows.size)
        assertEquals(ModuleProgressDao.STATUS_ASSESSED, rows.single().status)
    }

    @Test
    fun elProgresoSeRecuperaAlCerrarYReabrirLaBaseDeDatos() = runTest {
        // Incluye XP y logros (Fase 5).
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "reabrir-${System.nanoTime()}.db"
        fun open() = Room.databaseBuilder(context, FuncodeDatabase::class.java, name).allowMainThreadQueries().build()

        val first = open()
        val before = repositoryFor(first)
        before.recordAttempt(ExerciseAttempt("java.m01.l01.e01", correct = true, hintsUsed = 0))
        before.recordAssessmentResult("java.m01", AssessmentScore(correct = 7, total = 10))
        before.award(XpAward(exercise, 10))
        before.unlockAchievement("first_lesson")
        first.close()

        val second = open()
        try {
            val after = repositoryFor(second)
            assertEquals(setOf("java.m01.l01.e01"), after.observeSolvedExerciseIds().first())
            assertEquals(mapOf("java.m01" to 0.7), after.observeBestAssessmentScores().first())
            assertEquals(10, after.observeTotalXp().first())
            assertEquals(XpAwardResult.AlreadyGranted, after.award(XpAward(exercise, 10)))
            assertEquals(setOf("first_lesson"), after.observeUnlockedAchievements().first().keys)
        } finally {
            second.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun elPrimerAciertoDeCadaEjercicioConservaSusPistas() = runTest {
        repository.recordAttempt(ExerciseAttempt("a", correct = false, hintsUsed = 0))
        repository.recordAttempt(ExerciseAttempt("a", correct = true, hintsUsed = 2))
        repository.recordAttempt(ExerciseAttempt("a", correct = true, hintsUsed = 0))
        repository.recordAttempt(ExerciseAttempt("b", correct = true, hintsUsed = 1))
        repository.recordAttempt(ExerciseAttempt("c", correct = false, hintsUsed = 0))

        assertEquals(mapOf("a" to 2, "b" to 1), repository.firstCorrectAttempts())
    }

    @Test
    fun losMovimientosDeXpSeLeenConSuOrigen() = runTest {
        repository.award(XpAward(exercise, 10))
        repository.award(XpAward(XpSource(XpSourceType.LESSON, "java.m01.l01"), 20))

        val entries = repository.observeXpEntries().first()
        assertEquals(setOf(XpEntry(exercise, 10, 1_000L), XpEntry(XpSource(XpSourceType.LESSON, "java.m01.l01"), 20, 1_000L)), entries.toSet())
        assertEquals(entries.sumOf { it.amount }, repository.observeTotalXp().first())
    }

    @Test
    fun unLogroSoloSeGuardaUnaVez() = runTest {
        assertTrue(repository.unlockAchievement("first_lesson"))
        assertFalse(repository.unlockAchievement("first_lesson"))
        assertEquals(mapOf("first_lesson" to 1_000L), repository.observeUnlockedAchievements().first())
    }

    @Test
    fun elHistorialDeCadaEjercicioSeDeduceDeLosIntentos() = runTest {
        repository.recordAttempt(ExerciseAttempt("a", correct = false, hintsUsed = 0))
        repository.recordAttempt(ExerciseAttempt("a", correct = true, hintsUsed = 1))
        repository.recordAttempt(ExerciseAttempt("b", correct = true, hintsUsed = 0))

        val h = repository.observeExerciseHistories().first()
        assertEquals(ExerciseHistory("a", attempts = 2, failedBeforeFirstCorrect = true, hintsAtFirstCorrect = 1), h["a"])
        assertEquals(false, h.getValue("b").struggled)
    }
}
