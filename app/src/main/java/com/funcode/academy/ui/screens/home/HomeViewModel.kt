package com.funcode.academy.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.progress.ModuleProgress
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.rpg.LevelCurve
import com.funcode.academy.domain.rpg.LevelProgress
import com.funcode.academy.domain.rpg.RewardEngine
import com.funcode.academy.ui.progress.observeModulesProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Expone a la pantalla de Inicio el nivel (calculado desde el registro de XP) y el progreso de los módulos. */
class HomeViewModel(
    progressRepository: ProgressRepository,
    contentRepository: ContentRepository,
    rewardEngine: RewardEngine,
) : ViewModel() {

    /** `null` mientras se lee la base de datos por primera vez. */
    val level: StateFlow<LevelProgress?> = progressRepository.observeTotalXp()
        .map { LevelCurve.progressFor(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    /** `null` mientras se lee el contenido. */
    val modules: StateFlow<List<ModuleProgress>?> = observeModulesProgress(contentRepository, progressRepository)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    init {
        // Concede lo que hubiera quedado pendiente (p. ej. si la app se cerró justo
        // después de guardar un acierto). No duplica nada: el registro lo impide.
        viewModelScope.launch { runCatching { rewardEngine.sync() } }
    }

    companion object {
        fun factory(progressRepository: ProgressRepository, contentRepository: ContentRepository, rewardEngine: RewardEngine) =
            viewModelFactory { initializer { HomeViewModel(progressRepository, contentRepository, rewardEngine) } }
    }
}
