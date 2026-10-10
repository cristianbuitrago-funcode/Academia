package com.funcode.academy.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funcode.academy.domain.progress.ProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Expone a la pantalla de Inicio el XP total leído de la base de datos. */
class HomeViewModel(progressRepository: ProgressRepository) : ViewModel() {

    /** `null` mientras se lee la base de datos por primera vez. */
    val totalXp: StateFlow<Int?> = progressRepository.observeTotalXp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    companion object {
        fun factory(progressRepository: ProgressRepository) = viewModelFactory {
            initializer { HomeViewModel(progressRepository) }
        }
    }
}
