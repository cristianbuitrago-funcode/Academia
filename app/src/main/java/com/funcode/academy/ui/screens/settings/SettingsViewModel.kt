package com.funcode.academy.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funcode.academy.domain.model.Audience
import com.funcode.academy.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado de la pantalla de ajustes. */
sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Loaded(val audience: Audience?) : SettingsUiState
}

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.audience
        .map<Audience?, SettingsUiState> { SettingsUiState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    fun onAudienceSelected(audience: Audience) {
        viewModelScope.launch { settingsRepository.setAudience(audience) }
    }

    companion object {
        fun factory(settingsRepository: SettingsRepository) = viewModelFactory {
            initializer { SettingsViewModel(settingsRepository) }
        }
    }
}
