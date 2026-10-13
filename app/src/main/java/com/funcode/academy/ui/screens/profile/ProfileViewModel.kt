package com.funcode.academy.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.progress.XpEntry
import com.funcode.academy.domain.progress.XpSourceType
import com.funcode.academy.domain.rpg.Achievement
import com.funcode.academy.domain.rpg.LevelCurve
import com.funcode.academy.domain.rpg.LevelProgress
import com.funcode.academy.domain.rpg.MissionProgress
import com.funcode.academy.domain.rpg.RewardEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/** Recompensas de un tipo de actividad: cuántas y cuánto XP en total. */
data class RewardTotals(val count: Int, val xp: Int)

/** Logro con su momento de desbloqueo (`null` si sigue pendiente). */
data class AchievementStatus(val achievement: Achievement, val unlockedAt: Long?)

data class ProfileState(
    val level: LevelProgress,
    val rewards: Map<XpSourceType, RewardTotals>,
    val achievements: List<AchievementStatus>,
    val missions: List<MissionProgress> = emptyList(),
) {
    val unlockedCount: Int get() = achievements.count { it.unlockedAt != null }
}

/**
 * Todo sale del registro de XP, de la tabla de logros y de los resultados verificados
 * (para las misiones); nada se inventa ni se guarda aparte.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(progressRepository: ProgressRepository, rewardEngine: RewardEngine) : ViewModel() {

    /** Se recalculan cuando cambia cualquier resultado guardado (también un paso de reto sin XP). */
    private val missions = combine(
        progressRepository.observeSolvedExerciseIds(),
        progressRepository.observeBestAssessmentScores(),
        progressRepository.observeXpEntries(),
    ) { _, _, _ -> }.mapLatest { runCatching { rewardEngine.missions() }.getOrDefault(emptyList()) }

    val state: StateFlow<ProfileState?> = combine(
        progressRepository.observeXpEntries(),
        progressRepository.observeUnlockedAchievements(),
        missions,
    ) { entries, unlocked, missions -> profileStateOf(entries, unlocked, missions) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    companion object {
        fun factory(progressRepository: ProgressRepository, rewardEngine: RewardEngine) = viewModelFactory {
            initializer { ProfileViewModel(progressRepository, rewardEngine) }
        }

        fun profileStateOf(entries: List<XpEntry>, unlocked: Map<String, Long>, missions: List<MissionProgress> = emptyList()) =
            ProfileState(
                level = LevelCurve.progressFor(entries.sumOf { it.amount }),
                rewards = entries.groupBy { it.source.type }.mapValues { (_, list) -> RewardTotals(list.size, list.sumOf { it.amount }) },
                achievements = Achievement.entries.map { AchievementStatus(it, unlocked[it.id]) },
                missions = missions,
            )
    }
}
