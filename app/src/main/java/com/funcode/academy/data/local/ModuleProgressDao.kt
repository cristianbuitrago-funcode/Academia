package com.funcode.academy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ModuleProgressDao {

    @Query("SELECT * FROM module_progress WHERE moduleId = :moduleId")
    abstract suspend fun get(moduleId: String): ModuleProgressEntity?

    @Query("SELECT * FROM module_progress")
    abstract fun observeAll(): Flow<List<ModuleProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun put(entity: ModuleProgressEntity)

    /**
     * Guarda el resultado de una evaluación conservando solo el mejor. Se hace en una
     * transacción (leer y escribir juntos) porque el SQLite de Android 8 no admite UPSERT.
     */
    @Transaction
    open suspend fun recordAssessmentScore(moduleId: String, score: Double) {
        val current = get(moduleId)
        val best = maxOf(score, current?.bestAssessmentScore ?: 0.0)
        // COMPLETED = la evaluación se ha realizado al menos una vez. MASTERED queda
        // reservado hasta que se apruebe el umbral de aprobación (P6).
        put(ModuleProgressEntity(moduleId = moduleId, status = STATUS_ASSESSED, bestAssessmentScore = best))
    }

    companion object {
        const val STATUS_ASSESSED = "COMPLETED"
    }
}
