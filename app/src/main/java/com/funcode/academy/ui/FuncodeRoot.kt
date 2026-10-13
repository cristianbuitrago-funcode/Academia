package com.funcode.academy.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.settings.LearningPreferences
import com.funcode.academy.domain.settings.SettingsRepository
import com.funcode.academy.ui.theme.FuncodeTheme
import com.funcode.academy.ui.theme.ProvideTextScale
import com.funcode.academy.ui.theme.isDark

/** Raíz de la interfaz: aplica las preferencias de tema y tamaño de texto a toda la app. */
@Composable
fun FuncodeRoot(
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    contentRepository: ContentRepository,
) {
    val preferences by settingsRepository.preferences.collectAsStateWithLifecycle(initialValue = LearningPreferences())
    FuncodeTheme(darkTheme = preferences.themeMode.isDark(isSystemInDarkTheme())) {
        ProvideTextScale(preferences.textScale) {
            FuncodeAppUi(progressRepository, settingsRepository, contentRepository)
        }
    }
}
