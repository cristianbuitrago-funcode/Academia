package com.funcode.academy.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.funcode.academy.data.content.AssetContentRepository
import com.funcode.academy.data.local.FuncodeDatabase
import com.funcode.academy.data.progress.RoomProgressRepository
import com.funcode.academy.domain.lesson.AssessmentScore
import com.funcode.academy.domain.mission.MissionStatus
import com.funcode.academy.domain.progress.ExerciseAttempt
import com.funcode.academy.domain.progress.XpSourceType
import com.funcode.academy.domain.rpg.RewardEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Motor de recompensas con Room real (archivo) y el contenido real de `assets`:
 * las recompensas no se duplican y sobreviven a cerrar y reabrir la base de datos.
 */
@RunWith(AndroidJUnit4::class)
class RewardPersistenceTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "recompensas-${System.nanoTime()}.db"
    private val content = AssetContentRepository(context.assets)
    private val open = mutableListOf<FuncodeDatabase>()

    private fun openDatabase() = Room.databaseBuilder(context, FuncodeDatabase::class.java, name)
        .allowMainThreadQueries().build().also { open += it }

    private fun repositoryFor(db: FuncodeDatabase) =
        RoomProgressRepository(db.xpLedgerDao(), db.exerciseAttemptDao(), db.moduleProgressDao(), db.achievementDao())

    @After
    fun tearDown() {
        open.forEach { it.close() }
        context.deleteDatabase(name)
    }

    @Test
    fun lasRecompensasNoSeDuplicanYSobrevivenAReabrir() = runTest {
        val first = openDatabase()
        val progress = repositoryFor(first)
        val engine = RewardEngine(content, progress)
        // Lección 1 completa (5 ejercicios: dificultades 1, 1, 2, 2, 2) y evaluación aprobada.
        (1..5).forEach { progress.recordAttempt(ExerciseAttempt("java.m01.l01.e0$it", correct = true, hintsUsed = 0)) }
        progress.recordAssessmentResult("java.m01", AssessmentScore(9, 10))

        val outcome = engine.sync()
        // 10 + 10 + 15 + 15 + 15 + 20 (lección) + 100 (evaluación) = 185
        assertEquals(185, outcome.xpGained)
        assertEquals(2, outcome.levelAfter)
        assertTrue(engine.sync().granted.isEmpty())
        val achievements = progress.observeUnlockedAchievements().first().keys
        assertEquals(setOf("first_lesson", "first_assessment_passed", "level_2"), achievements)
        first.close()

        val second = openDatabase()
        val reopened = repositoryFor(second)
        assertEquals(185, reopened.observeTotalXp().first())
        assertEquals(achievements, reopened.observeUnlockedAchievements().first().keys)
        // Un motor nuevo (como tras reiniciar la app) tampoco concede nada otra vez.
        val again = RewardEngine(content, reopened).sync()
        assertTrue(again.granted.isEmpty())
        assertTrue(again.newAchievements.isEmpty())
        assertEquals(7, reopened.observeXpEntries().first().size)
    }

    @Test
    fun unaRecompensaQueNoLlegoAGuardarseSeConcedeEnLaSiguienteSincronizacion() = runTest {
        val db = openDatabase()
        val progress = repositoryFor(db)
        // El acierto se guardó, pero la app se cerró antes de sincronizar.
        progress.recordAttempt(ExerciseAttempt("java.m01.l01.e01", correct = true, hintsUsed = 1))
        assertEquals(0, progress.observeTotalXp().first())

        assertEquals(7, RewardEngine(content, progress).sync().xpGained)
        assertEquals(7, progress.observeTotalXp().first())
    }

    @Test
    fun laBonificacionDeUnaMisionSeConcedeUnaVezYSobreviveAReabrir() = runTest {
        val first = openDatabase()
        val progress = repositoryFor(first)
        // Misión 1: lecciones 1 y 2 completas.
        listOf("java.m01.l01" to 5, "java.m01.l02" to 6).forEach { (lesson, count) ->
            (1..count).forEach { progress.recordAttempt(ExerciseAttempt("$lesson.e0$it", correct = true, hintsUsed = 0)) }
        }
        val outcome = RewardEngine(content, progress).sync()
        assertEquals(listOf("java.m01.mi01"), outcome.completedMissions.map { it.id })
        first.close()

        val second = openDatabase()
        val reopened = repositoryFor(second)
        val again = RewardEngine(content, reopened)
        assertTrue(again.sync().granted.isEmpty())
        val missionEntries = reopened.observeXpEntries().first().filter { it.source.type == XpSourceType.MISSION }
        assertEquals(listOf(10), missionEntries.map { it.amount })
        assertEquals(MissionStatus.COMPLETED, again.missions().first().status)
    }
}
