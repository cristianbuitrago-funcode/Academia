package com.funcode.academy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    /** Guarda un logro. Si ya existía (clave primaria), se ignora y devuelve -1. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AchievementUnlockedEntity): Long

    @Query("SELECT * FROM achievement_unlocked")
    fun observeAll(): Flow<List<AchievementUnlockedEntity>>
}
