package com.funcode.academy.ui.screens.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.ModuleLoadResult
import com.funcode.academy.domain.progress.ModuleProgress
import com.funcode.academy.domain.progress.ProgressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

sealed interface ModuleUiState {
    data object Loading : ModuleUiState
    data object NotFound : ModuleUiState
    data object InvalidContent : ModuleUiState
    data class Loaded(val progress: ModuleProgress) : ModuleUiState
}

/** Un módulo con sus lecciones en orden, la evaluación y el progreso guardado. */
class ModuleViewModel(moduleId: String, content: ContentRepository, progress: ProgressRepository) : ViewModel() {

    val uiState: StateFlow<ModuleUiState> = combine(
        flow { emit(content.loadModule(moduleId)) },
        progress.observeSolvedExerciseIds(),
        progress.observeBestAssessmentScores(),
    ) { result, solved, scores ->
        when (result) {
            is ModuleLoadResult.Loaded -> ModuleUiState.Loaded(ModuleProgress.of(result.module, solved, scores))
            ModuleLoadResult.NotFound -> ModuleUiState.NotFound
            is ModuleLoadResult.Invalid -> ModuleUiState.InvalidContent
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModuleUiState.Loading)

    companion object {
        fun factory(moduleId: String, content: ContentRepository, progress: ProgressRepository) = viewModelFactory {
            initializer { ModuleViewModel(moduleId, content, progress) }
        }
    }
}
