package com.funcode.academy.data.progress

import com.funcode.academy.data.local.AchievementDao
import com.funcode.academy.data.local.AchievementUnlockedEntity
import com.funcode.academy.data.local.ExerciseAttemptDao
import com.funcode.academy.data.local.ExerciseAttemptEntity
import com.funcode.academy.data.local.ModuleProgressDao
import com.funcode.academy.data.local.XpLedgerDao
import com.funcode.academy.data.local.XpLedgerEntity
import com.funcode.academy.domain.lesson.AssessmentScore
import com.funcode.academy.domain.progress.ExerciseAttempt
import com.funcode.academy.domain.progress.ExerciseHistory
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.progress.XpAward
import com.funcode.academy.domain.progress.XpAwardResult
import com.funcode.academy.domain.progress.XpEntry
import com.funcode.academy.domain.progress.XpSource
import com.funcode.academy.domain.progress.XpSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementación de [ProgressRepository] sobre Room. */
class RoomProgressRepository(
    private val xpLedgerDao: XpLedgerDao,
    private val exerciseAttemptDao: ExerciseAttemptDao,
    private val moduleProgressDao: ModuleProgressDao,
    private val achievementDao: AchievementDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : ProgressRepository {

    override fun observeTotalXp(): Flow<Int> = xpLedgerDao.observeTotalXp()

    override suspend fun award(award: XpAward): XpAwardResult {
        val rowId = xpLedgerDao.insert(
            XpLedgerEntity(
                sourceType = award.source.type.name,
                sourceId = award.source.id,
                amount = award.amount,
                timestamp = clock(),
            ),
        )
        return if (rowId == -1L) XpAwardResult.AlreadyGranted else XpAwardResult.Granted(award.amount)
    }

    override suspend fun recordAttempt(attempt: ExerciseAttempt) {
        exerciseAttemptDao.insert(
            ExerciseAttemptEntity(
                exerciseId = attempt.exerciseId,
                timestamp = clock(),
                correct = attempt.correct,
                hintsUsed = attempt.hintsUsed,
            ),
        )
    }

    override fun observeSolvedExerciseIds(): Flow<Set<String>> =
        exerciseAttemptDao.observeSolvedExerciseIds().map { it.toSet() }

    override suspend fun recordAssessmentResult(moduleId: String, score: AssessmentScore) =
        moduleProgressDao.recordAssessmentScore(moduleId, score.fraction)

    override fun observeBestAssessmentScores(): Flow<Map<String, Double>> =
        moduleProgressDao.observeAll().map { rows ->
            rows.mapNotNull { row -> row.bestAssessmentScore?.let { row.moduleId to it } }.toMap()
        }

    override suspend fun firstCorrectAttempts(): Map<String, Int> =
        exerciseAttemptDao.firstCorrectAttempts().associate { it.exerciseId to it.hintsUsed }

    override fun observeXpEntries(): Flow<List<XpEntry>> = xpLedgerDao.observeAll().map { rows ->
        rows.mapNotNull { row ->
            // Un tipo desconocido (p. ej. de una versión futura) se ignora en lugar de cerrar la app.
            val type = XpSourceType.entries.firstOrNull { it.name == row.sourceType } ?: return@mapNotNull null
            XpEntry(XpSource(type, row.sourceId), row.amount, row.timestamp)
        }
    }

    override suspend fun unlockAchievement(achievementId: String): Boolean =
        achievementDao.insert(AchievementUnlockedEntity(achievementId, unlockedAt = clock())) != -1L

    override fun observeUnlockedAchievements(): Flow<Map<String, Long>> =
        achievementDao.observeAll().map { rows -> rows.associate { it.achievementId to it.unlockedAt } }

    override fun observeExerciseHistories(): Flow<Map<String, ExerciseHistory>> =
        exerciseAttemptDao.observeAll().map { rows ->
            ExerciseHistory.fromAttempts(rows.map { ExerciseAttempt(it.exerciseId, it.correct, it.hintsUsed) })
        }
}
