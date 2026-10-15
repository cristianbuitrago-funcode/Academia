package com.funcode.academy

import android.content.Context
import androidx.room.Room
import com.funcode.academy.data.content.AssetContentRepository
import com.funcode.academy.data.local.FuncodeDatabase
import com.funcode.academy.data.progress.RoomProgressRepository
import com.funcode.academy.data.settings.DataStoreSettingsRepository
import com.funcode.academy.data.settings.settingsDataStore
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.runner.CodeRunner
import com.funcode.academy.domain.settings.SettingsRepository
import com.funcode.academy.runner.IsolatedJavaRunner

/**
 * Inyección de dependencias manual: crea las implementaciones concretas
 * (Room, DataStore) y las expone como interfaces del dominio.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: FuncodeDatabase by lazy {
        Room.databaseBuilder(appContext, FuncodeDatabase::class.java, FuncodeDatabase.NAME).build()
    }

    val progressRepository: ProgressRepository by lazy {
        RoomProgressRepository(
            database.xpLedgerDao(),
            database.exerciseAttemptDao(),
            database.moduleProgressDao(),
            database.achievementDao(),
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(appContext.settingsDataStore)
    }

    /** Contenido educativo empaquetado en la app (sin conexión). */
    val contentRepository: ContentRepository by lazy {
        AssetContentRepository(appContext.assets)
    }

    /** PROTOTIPO Fase 8: ejecución de Java en un proceso aislado (solo laboratorio de depuración). */
    val codeRunner: CodeRunner by lazy { IsolatedJavaRunner(appContext) }
}
