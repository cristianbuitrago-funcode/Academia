package com.funcode.academy.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Esquema inicial del progreso (plan técnico §6.3). Los enumerados se guardan
// como texto (su nombre) para que la base de datos sea legible y estable.
// profile, exercise_progress y lesson_progress siguen sin uso (F3-D2).

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String?,
    val audience: String,
    val avatarId: String?,
    val createdAt: Long,
)

@Entity(tableName = "exercise_attempt", indices = [Index("exerciseId")])
data class ExerciseAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val timestamp: Long,
    val correct: Boolean,
    val hintsUsed: Int,
)

@Entity(tableName = "exercise_progress")
data class ExerciseProgressEntity(
    @PrimaryKey val exerciseId: String,
    val firstSolvedAt: Long,
    val hintsUsedOnFirstSolve: Int,
)

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val status: String,
    val completedAt: Long?,
)

@Entity(tableName = "module_progress")
data class ModuleProgressEntity(
    @PrimaryKey val moduleId: String,
    val status: String,
    val bestAssessmentScore: Double?,
)

/**
 * Registro de XP. El índice único sobre (sourceType, sourceId) impide que una
 * misma actividad conceda XP más de una vez (condición permanente 4).
 */
@Entity(
    tableName = "xp_ledger",
    indices = [Index(value = ["sourceType", "sourceId"], unique = true)],
)
data class XpLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceType: String,
    val sourceId: String,
    val amount: Int,
    val timestamp: Long,
)

@Entity(tableName = "achievement_unlocked")
data class AchievementUnlockedEntity(
    @PrimaryKey val achievementId: String,
    val unlockedAt: Long,
)
