package com.funcode.academy.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funcode.academy.domain.model.Audience
import com.funcode.academy.domain.settings.LearningPreferences
import com.funcode.academy.domain.settings.SettingsRepository
import com.funcode.academy.domain.settings.TextScale
import com.funcode.academy.domain.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la pantalla de ajustes. */
sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Loaded(val preferences: LearningPreferences) : SettingsUiState {
        val audience: Audience? get() = preferences.audience
    }
}

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.preferences
        .map<LearningPreferences, SettingsUiState> { SettingsUiState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    fun onAudienceSelected(audience: Audience) = launch { setAudience(audience) }

    fun onThemeSelected(mode: ThemeMode) = launch { setThemeMode(mode) }

    fun onTextScaleSelected(scale: TextScale) = launch { setTextScale(scale) }

    fun onHintsChanged(enabled: Boolean) = launch { setHintsEnabled(enabled) }

    /** Solo preferencias: el progreso (Room) no se toca. */
    fun onResetPreferences() = launch { resetPreferences() }

    private fun launch(action: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settingsRepository.action() }
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository) = viewModelFactory {
            initializer { SettingsViewModel(settingsRepository) }
        }
    }
}
