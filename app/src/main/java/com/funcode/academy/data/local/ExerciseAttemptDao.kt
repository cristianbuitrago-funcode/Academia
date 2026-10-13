package com.funcode.academy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseAttemptDao {

    @Insert
    suspend fun insert(attempt: ExerciseAttemptEntity): Long

    /** Intentos de un ejercicio, del más antiguo al más reciente. */
    @Query("SELECT * FROM exercise_attempt WHERE exerciseId = :exerciseId ORDER BY timestamp, id")
    suspend fun attemptsFor(exerciseId: String): List<ExerciseAttemptEntity>

    /** Ejercicios con al menos un intento correcto. */
    @Query("SELECT DISTINCT exerciseId FROM exercise_attempt WHERE correct = 1")
    fun observeSolvedExerciseIds(): Flow<List<String>>

    /**
     * Primer intento correcto de cada ejercicio (el de menor id, que es el más antiguo).
     * Sus pistas deciden el XP del ejercicio.
     */
    @Query(
        "SELECT exerciseId, hintsUsed FROM exercise_attempt WHERE id IN " +
            "(SELECT MIN(id) FROM exercise_attempt WHERE correct = 1 GROUP BY exerciseId)",
    )
    suspend fun firstCorrectAttempts(): List<FirstCorrectAttempt>
}

data class FirstCorrectAttempt(val exerciseId: String, val hintsUsed: Int)
