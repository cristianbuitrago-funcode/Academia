package com.funcode.academy.domain.rpg

import com.funcode.academy.domain.lesson.AssessmentScore
import com.funcode.academy.domain.lesson.ContentFiles
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.CourseModule
import com.funcode.academy.domain.lesson.LessonLoadResult
import com.funcode.academy.domain.lesson.LessonSummary
import com.funcode.academy.domain.lesson.ModuleLoadResult
import com.funcode.academy.domain.lesson.toSummary
import com.funcode.academy.domain.mission.MissionStatus
import com.funcode.academy.domain.progress.ExerciseAttempt
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.progress.XpAward
import com.funcode.academy.domain.progress.XpAwardResult
import com.funcode.academy.domain.progress.XpEntry
import com.funcode.academy.domain.progress.XpSourceType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Motor de recompensas con el contenido real del módulo 1 y un registro en memoria. */
class RewardEngineTest {

    private val content = object : ContentRepository {
        override suspend fun listLessons(): List<LessonSummary> = ContentFiles.lessons.map { it.toSummary() }
        override suspend fun loadLesson(lessonId: String) =
            ContentFiles.lessons.firstOrNull { it.id == lessonId }?.let { LessonLoadResult.Loaded(it) } ?: LessonLoadResult.NotFound
        override suspend fun listModules(): List<CourseModule> = ContentFiles.modules
        override suspend fun loadModule(moduleId: String) =
            ContentFiles.modules.firstOrNull { it.id == moduleId }?.let { ModuleLoadResult.Loaded(it) } ?: ModuleLoadResult.NotFound
        override suspend fun listMissions() = ContentFiles.missions
    }

    /** Registro en memoria con las mismas garantías que Room: una entrada por actividad. */
    private class MemoryProgress : ProgressRepository {
        val ledger = MutableStateFlow(emptyList<XpEntry>())
        val attempts = mutableListOf<ExerciseAttempt>()
        val scores = MutableStateFlow(emptyMap<String, Double>())
        val achievements = MutableStateFlow(emptyMap<String, Long>())

        override fun observeTotalXp(): Flow<Int> = ledger.map { l -> l.sumOf { it.amount } }
        override suspend fun award(award: XpAward): XpAwardResult {
            if (ledger.value.any { it.source == award.source }) return XpAwardResult.AlreadyGranted
            ledger.update { listOf(XpEntry(award.source, award.amount, 0)) + it }
            return XpAwardResult.Granted(award.amount)
        }
        override suspend fun recordAttempt(attempt: ExerciseAttempt) { attempts += attempt }
        override fun observeSolvedExerciseIds(): Flow<Set<String>> = MutableStateFlow(attempts.filter { it.correct }.map { it.exerciseId }.toSet())
        override suspend fun recordAssessmentResult(moduleId: String, score: AssessmentScore) =
            scores.update { it + (moduleId to maxOf(score.fraction, it[moduleId] ?: 0.0)) }
        override fun observeBestAssessmentScores(): Flow<Map<String, Double>> = scores
        override suspend fun firstCorrectAttempts(): Map<String, Int> =
            attempts.filter { it.correct }.groupBy { it.exerciseId }.mapValues { it.value.first().hintsUsed }
        override fun observeXpEntries(): Flow<List<XpEntry>> = ledger
        override suspend fun unlockAchievement(achievementId: String): Boolean {
            if (achievementId in achievements.value) return false
            achievements.update { it + (achievementId to 1L) }
            return true
        }
        override fun observeUnlockedAchievements(): Flow<Map<String, Long>> = achievements
    }

    private val progress = MemoryProgress()
    private val engine = RewardEngine(content, progress)
    private val lessons = ContentFiles.modules.single().lessons.map { id -> ContentFiles.lessons.single { it.id == id.id } }

    private fun solve(exerciseId: String, hints: Int = 0) {
        progress.attempts += ExerciseAttempt(exerciseId, correct = true, hintsUsed = hints)
    }

    private fun solveLesson(index: Int) = lessons[index].exercises.forEach { solve(it.id) }

    @Test fun `estado inicial sin recompensas`() = runTest {
        val outcome = engine.sync()
        assertTrue(outcome.granted.isEmpty())
        assertTrue(outcome.newAchievements.isEmpty())
        assertEquals(1, outcome.levelAfter)
    }

    @Test fun `un fallo no concede XP`() = runTest {
        progress.recordAttempt(ExerciseAttempt("java.m01.l01.e01", correct = false, hintsUsed = 0))
        assertTrue(engine.sync().granted.isEmpty())
    }

    @Test fun `el primer acierto concede XP una sola vez aunque se repita`() = runTest {
        solve("java.m01.l01.e01") // dificultad 1
        assertEquals(10, engine.sync().xpGained)
        solve("java.m01.l01.e01")
        assertEquals(0, engine.sync().xpGained)
        assertEquals(1, progress.ledger.value.size)
    }

    @Test fun `las pistas del primer acierto deciden el XP aunque luego se resuelva sin ellas`() = runTest {
        solve("java.m01.l01.e01", hints = 1)
        solve("java.m01.l01.e01", hints = 0)
        assertEquals(7, engine.sync().xpGained)
    }

    @Test fun `completar una leccion da su bonificacion y el primer logro`() = runTest {
        solveLesson(0)
        val outcome = engine.sync()
        val expected = lessons[0].exercises.sumOf { RewardRules.exerciseXp(it.difficulty, 0) } + RewardRules.LESSON_XP
        assertEquals(expected, outcome.xpGained)
        assertTrue(outcome.granted.any { it.source.type == XpSourceType.LESSON && it.source.id == "java.m01.l01" })
        assertEquals(listOf(Achievement.FIRST_LESSON), outcome.newAchievements)
    }

    @Test fun `la evaluacion aprobada se recompensa una vez y la no aprobada nunca`() = runTest {
        progress.recordAssessmentResult("java.m01", AssessmentScore(7, 10))
        assertEquals(0, engine.sync().xpGained)
        progress.recordAssessmentResult("java.m01", AssessmentScore(8, 10))
        val outcome = engine.sync()
        assertEquals(RewardRules.ASSESSMENT_PASS_XP, outcome.xpGained)
        assertTrue(Achievement.FIRST_ASSESSMENT_PASSED in outcome.newAchievements)
        assertTrue(Achievement.LEVEL_2 in outcome.newAchievements)
        assertTrue(outcome.leveledUp)
        progress.recordAssessmentResult("java.m01", AssessmentScore(10, 10))
        assertEquals(0, engine.sync().xpGained)
    }

    @Test fun `las preguntas de la evaluacion no dan XP por separado`() = runTest {
        solve("java.m01.eval.q01")
        assertEquals(0, engine.sync().xpGained)
    }

    @Test fun `el modulo completo sin el proyecto suma 910 XP mas 60 de seis misiones y llega al nivel 4`() = runTest {
        lessons.indices.forEach { solveLesson(it) }
        progress.recordAssessmentResult("java.m01", AssessmentScore(10, 10))
        val outcome = engine.sync()
        assertEquals(910 + 6 * RewardRules.MISSION_XP, outcome.xpGained)
        assertEquals((1..6).map { "java.m01.mi0$it" }, outcome.completedMissions.map { it.id })
        assertEquals(4, outcome.levelAfter)
        assertEquals(Achievement.entries.toSet() - Achievement.FIRST_CHALLENGE, outcome.newAchievements.toSet())
        // Sincronizar otra vez no cambia nada.
        val again = engine.sync()
        assertTrue(again.granted.isEmpty())
        assertTrue(again.newAchievements.isEmpty())
        assertFalse(again.leveledUp)
    }

    @Test fun `llamadas simultaneas no duplican recompensas`() = runTest {
        solveLesson(0)
        val outcomes = (1..5).map { async { engine.sync() } }.awaitAll()
        assertEquals(1, outcomes.count { it.granted.isNotEmpty() })
        assertEquals(progress.ledger.value.map { it.source }.distinct().size, progress.ledger.value.size)
        assertEquals(1, progress.achievements.value.size)
    }

    private val challenge = ContentFiles.lessons.single { it.id == "java.m01.reto" }

    @Test fun `el reto resuelto antes de completar las lecciones no da XP`() = runTest {
        challenge.exercises.forEach { solve(it.id) }
        assertEquals(0, engine.sync().xpGained)
    }

    @Test fun `el modulo completo con el proyecto y las siete misiones suma 1030 XP y llega al nivel 5`() = runTest {
        lessons.indices.forEach { solveLesson(it) }
        progress.recordAssessmentResult("java.m01", AssessmentScore(10, 10))
        assertEquals(970, engine.sync().xpGained)
        challenge.exercises.forEach { solve(it.id, hints = 3) } // las pistas no afectan al XP del reto
        val outcome = engine.sync()
        assertEquals(RewardRules.CHALLENGE_XP + RewardRules.MISSION_XP, outcome.xpGained)
        assertEquals(listOf("java.m01.mi07"), outcome.completedMissions.map { it.id })
        assertEquals(listOf(Achievement.FIRST_CHALLENGE), outcome.newAchievements)
        assertEquals(1030, progress.ledger.value.sumOf { it.amount })
        assertEquals(5, outcome.levelAfter)
        assertTrue(engine.sync().granted.isEmpty())
        assertTrue(engine.missions().all { it.status == MissionStatus.COMPLETED })
    }

    @Test fun `las misiones avanzan en cadena y su bonificacion no se repite`() = runTest {
        assertEquals(
            listOf(MissionStatus.AVAILABLE) + List(6) { MissionStatus.LOCKED },
            engine.missions().map { it.status },
        )
        solveLesson(0)
        engine.sync()
        assertEquals(MissionStatus.IN_PROGRESS, engine.missions().first().status)
        solveLesson(1)
        val outcome = engine.sync()
        assertEquals(listOf("java.m01.mi01"), outcome.completedMissions.map { it.id })
        val states = engine.missions().map { it.status }
        // mi02 ya tiene progreso: incluye un ejercicio de ordenar de la lección 2.
        assertEquals(listOf(MissionStatus.COMPLETED, MissionStatus.IN_PROGRESS, MissionStatus.LOCKED), states.take(3))
        // Repetir ejercicios de la misión completada no da más bonificación.
        solveLesson(0)
        assertTrue(engine.sync().granted.isEmpty())
        assertEquals(1, progress.ledger.value.count { it.source.type == XpSourceType.MISSION })
    }
}
