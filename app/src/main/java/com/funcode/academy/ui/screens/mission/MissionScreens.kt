package com.funcode.academy.ui.screens.mission

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funcode.academy.R
import com.funcode.academy.ui.components.NeonCard
import com.funcode.academy.ui.components.NeonProgressBar
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.ReviewStatus
import com.funcode.academy.domain.mission.MissionProgress
import com.funcode.academy.domain.mission.MissionStatus
import com.funcode.academy.domain.mission.MissionTask
import com.funcode.academy.domain.mission.MissionType
import com.funcode.academy.domain.mission.TaskProgress
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.rpg.RewardEngine
import com.funcode.academy.domain.rpg.RewardRules
import com.funcode.academy.ui.components.DraftBanner

@Composable
private fun missionsViewModel(content: ContentRepository, progress: ProgressRepository, engine: RewardEngine): MissionsViewModel =
    viewModel(key = "missions", factory = MissionsViewModel.factory(content, progress, engine))

@Composable
fun MissionsRoute(content: ContentRepository, progress: ProgressRepository, engine: RewardEngine, onOpenMission: (String) -> Unit) {
    val state by missionsViewModel(content, progress, engine).state.collectAsStateWithLifecycle()
    MissionsScreen(state, onOpenMission)
}

@Composable
fun MissionRoute(
    missionId: String,
    content: ContentRepository,
    progress: ProgressRepository,
    engine: RewardEngine,
    onOpenActivity: (String) -> Unit,
    onExit: () -> Unit,
) {
    val state by missionsViewModel(content, progress, engine).state.collectAsStateWithLifecycle()
    MissionDetailScreen(state, missionId, onOpenActivity, onExit)
}

/** Lista de misiones con su estado. */
@Composable
fun MissionsScreen(state: MissionsState?, onOpenMission: (String) -> Unit, modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        Heading(stringResource(R.string.missions_title), large = true)
        Text(stringResource(R.string.missions_intro), style = MaterialTheme.typography.bodyMedium)
        when {
            state == null -> Unit
            state.missions.isEmpty() -> Text(stringResource(R.string.missions_none), style = MaterialTheme.typography.bodyLarge)
            else -> state.missions.forEach { MissionCard(it, onOpenMission) }
        }
    }
}

@Composable
private fun MissionCard(progress: MissionProgress, onOpenMission: (String) -> Unit) {
    val mission = progress.mission
    // La tarjeta entera es el botón: TalkBack lee tipo, título y estado juntos.
    NeonCard(
        onClick = { onOpenMission(mission.id) },
        colors = if (progress.status == MissionStatus.COMPLETED) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(typeRes(mission.type)), style = MaterialTheme.typography.labelMedium)
            Text(mission.title, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(statusRes(progress.status)), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(R.string.mission_tasks_progress, progress.completedTasks, progress.tasks.size),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Detalle: objetivo, requisitos, tareas con enlace a su actividad, recompensa y resultado. */
@Composable
fun MissionDetailScreen(
    state: MissionsState?,
    missionId: String,
    onOpenActivity: (String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenColumn(modifier) {
        if (state == null) return@ScreenColumn
        val progress = state.find(missionId)
        if (progress == null) {
            Text(stringResource(R.string.mission_not_found), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onExit) { Text(stringResource(R.string.mission_back)) }
            return@ScreenColumn
        }
        val mission = progress.mission
        Text(stringResource(typeRes(mission.type)), style = MaterialTheme.typography.labelLarge)
        Heading(mission.title, large = true)
        if (mission.reviewStatus == ReviewStatus.DRAFT) DraftBanner()
        Text(stringResource(statusRes(progress.status)), style = MaterialTheme.typography.titleMedium)
        Text(mission.description, style = MaterialTheme.typography.bodyLarge)
        Heading(stringResource(R.string.mission_objective_title))
        Text(mission.objective, style = MaterialTheme.typography.bodyLarge)

        if (progress.status == MissionStatus.LOCKED) {
            NeonCard(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.mission_locked_reason, progress.missingPrerequisites.joinToString { "«${it.title}»" }),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Heading(stringResource(R.string.mission_tasks_title))
        progress.tasks.forEach { TaskCard(it, state.activityTitles, locked = progress.status == MissionStatus.LOCKED, onOpenActivity) }

        Heading(stringResource(R.string.mission_reward_title))
        Text(
            if (progress.status == MissionStatus.COMPLETED) stringResource(R.string.mission_reward_obtained, RewardRules.MISSION_XP)
            else stringResource(R.string.mission_reward_pending, RewardRules.MISSION_XP),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (progress.status == MissionStatus.COMPLETED) {
            NeonCard(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.mission_completed_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.mission_completed_summary, progress.tasks.size), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.mission_practice_note), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.mission_back)) }
    }
}

@Composable
private fun TaskCard(task: TaskProgress, titles: Map<String, String>, locked: Boolean, onOpenActivity: (String) -> Unit) {
    val activity = titles[task.activityId] ?: task.activityId
    val text = when (val t = task.task) {
        is MissionTask.CompleteLesson -> stringResource(R.string.mission_task_lesson, activity)
        is MissionTask.SolveExercises -> t.description
        is MissionTask.PassAssessment -> stringResource(R.string.mission_task_assessment, activity)
        is MissionTask.CompleteChallenge -> stringResource(R.string.mission_task_project, activity)
    }
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.semantics(mergeDescendants = true) {}) {
                Text(text, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (task.done) stringResource(R.string.mission_task_done)
                    else stringResource(R.string.mission_task_progress, task.current, task.total),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            // La barra es decorativa: el texto anterior ya da el avance exacto.
            NeonProgressBar(task.current.toFloat() / task.total)
            // Con la misión bloqueada no se ofrece el botón: primero hay que cumplir los requisitos.
            if (!locked) {
                Button(onClick = { onOpenActivity(task.activityId) }) {
                    Text(stringResource(if (task.done) R.string.mission_review_activity else R.string.mission_open_activity, activity))
                }
            }
        }
    }
}

@StringRes
fun typeRes(type: MissionType): Int = when (type) {
    MissionType.LEARNING -> R.string.mission_type_learning
    MissionType.PRACTICE -> R.string.mission_type_practice
    MissionType.ASSESSMENT -> R.string.mission_type_assessment
    MissionType.PROJECT -> R.string.mission_type_project
}

@StringRes
fun statusRes(status: MissionStatus): Int = when (status) {
    MissionStatus.LOCKED -> R.string.mission_status_locked
    MissionStatus.AVAILABLE -> R.string.mission_status_available
    MissionStatus.IN_PROGRESS -> R.string.mission_status_in_progress
    MissionStatus.COMPLETED -> R.string.mission_status_completed
}

@Composable
private fun ScreenColumn(modifier: Modifier, content: @Composable () -> Unit) = Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) { content() }

@Composable
private fun Heading(text: String, large: Boolean = false) = Text(
    text = text,
    style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
    modifier = Modifier.semantics { heading() },
)
