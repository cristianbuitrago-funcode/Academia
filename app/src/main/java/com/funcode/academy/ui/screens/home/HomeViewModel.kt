package com.funcode.academy.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.progress.ModuleProgress
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.ui.progress.observeModulesProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Expone a la pantalla de Inicio el XP total y el progreso de los módulos. */
class HomeViewModel(
    progressRepository: ProgressRepository,
    contentRepository: ContentRepository,
) : ViewModel() {

    /** `null` mientras se lee la base de datos por primera vez. */
    val totalXp: StateFlow<Int?> = progressRepository.observeTotalXp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    /** `null` mientras se lee el contenido. */
    val modules: StateFlow<List<ModuleProgress>?> = observeModulesProgress(contentRepository, progressRepository)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    companion object {
        fun factory(progressRepository: ProgressRepository, contentRepository: ContentRepository) = viewModelFactory {
            initializer { HomeViewModel(progressRepository, contentRepository) }
        }
    }
}
