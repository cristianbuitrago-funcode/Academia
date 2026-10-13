package com.funcode.academy.ui.screens.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.mission.MissionProgress
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.rpg.RewardEngine
import com.funcode.academy.ui.progress.observeMissions
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Misiones con su estado y los títulos de las actividades a las que enlazan. */
data class MissionsState(val missions: List<MissionProgress>, val activityTitles: Map<String, String>) {
    fun find(missionId: String): MissionProgress? = missions.firstOrNull { it.mission.id == missionId }
}

class MissionsViewModel(
    content: ContentRepository,
    progress: ProgressRepository,
    rewardEngine: RewardEngine,
) : ViewModel() {

    /** `null` mientras se lee el contenido. */
    val state: StateFlow<MissionsState?> = combine(
        observeMissions(progress, rewardEngine),
        flow { emit(content.listLessons().associate { it.id to it.title }) },
    ) { missions, titles -> MissionsState(missions, titles) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    init {
        // Concede las bonificaciones pendientes (idempotente: nunca duplica).
        viewModelScope.launch { runCatching { rewardEngine.sync() } }
    }

    companion object {
        fun factory(content: ContentRepository, progress: ProgressRepository, rewardEngine: RewardEngine) = viewModelFactory {
            initializer { MissionsViewModel(content, progress, rewardEngine) }
        }
    }
}
