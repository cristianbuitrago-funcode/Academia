package com.funcode.academy.ui.screens.module

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

/** Módulos con su progreso, para el Mapa y para Inicio. `null` mientras se cargan. */
class ModuleListViewModel(content: ContentRepository, progress: ProgressRepository) : ViewModel() {

    val modules: StateFlow<List<ModuleProgress>?> = observeModulesProgress(content, progress)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    companion object {
        fun factory(content: ContentRepository, progress: ProgressRepository) = viewModelFactory {
            initializer { ModuleListViewModel(content, progress) }
        }
    }
}
