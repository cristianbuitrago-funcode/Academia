package com.funcode.academy.ui.progress

import com.funcode.academy.domain.mission.MissionProgress
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.rpg.RewardEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest

/**
 * Estado de las misiones, recalculado cada vez que cambia un resultado guardado
 * (intentos, evaluaciones o XP). No se guarda aparte: siempre se deduce de Room.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun observeMissions(progress: ProgressRepository, engine: RewardEngine): Flow<List<MissionProgress>> = combine(
    progress.observeSolvedExerciseIds(),
    progress.observeBestAssessmentScores(),
    progress.observeXpEntries(),
) { _, _, _ -> }.mapLatest { runCatching { engine.missions() }.getOrDefault(emptyList()) }
