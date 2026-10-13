package com.funcode.academy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base de datos local: fuente de verdad del progreso.
 * Cualquier cambio de esquema exige subir [version] y escribir una migración.
 */
@Database(
    entities = [
        ProfileEntity::class,
        ExerciseAttemptEntity::class,
        ExerciseProgressEntity::class,
        LessonProgressEntity::class,
        ModuleProgressEntity::class,
        XpLedgerEntity::class,
        AchievementUnlockedEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FuncodeDatabase : RoomDatabase() {

    abstract fun xpLedgerDao(): XpLedgerDao

    abstract fun exerciseAttemptDao(): ExerciseAttemptDao

    companion object {
        const val NAME = "funcode.db"
    }
}
