package com.funcode.academy.data.progress

import com.funcode.academy.data.local.ExerciseAttemptDao
import com.funcode.academy.data.local.ExerciseAttemptEntity
import com.funcode.academy.data.local.XpLedgerDao
import com.funcode.academy.data.local.XpLedgerEntity
import com.funcode.academy.domain.progress.ExerciseAttempt
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.progress.XpAward
import com.funcode.academy.domain.progress.XpAwardResult
import kotlinx.coroutines.flow.Flow

/** Implementación de [ProgressRepository] sobre Room. */
class RoomProgressRepository(
    private val xpLedgerDao: XpLedgerDao,
    private val exerciseAttemptDao: ExerciseAttemptDao,
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
}
