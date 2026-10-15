package com.funcode.academy.ui.screens.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.ReviewStatus
import com.funcode.academy.domain.progress.ChallengeProgress
import com.funcode.academy.domain.progress.LessonProgress
import com.funcode.academy.domain.progress.ModuleProgress
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.ui.components.DraftBanner
import kotlin.math.roundToInt

/** Mapa de aprendizaje: lista de módulos con su progreso. */
@Composable
fun ModuleListRoute(
    content: ContentRepository,
    progress: ProgressRepository,
    onOpenModule: (String) -> Unit,
    onOpenMissions: () -> Unit,
) {
    val viewModel: ModuleListViewModel = viewModel(factory = ModuleListViewModel.factory(content, progress))
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    ModuleListScreen(modules, onOpenModule, onOpenMissions)
}

@Composable
fun ModuleListScreen(
    modules: List<ModuleProgress>?,
    onOpenModule: (String) -> Unit,
    onOpenMissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenColumn(modifier) {
        Heading(stringResource(R.string.map_title), large = true)
        OutlinedButton(onClick = onOpenMissions) { Text(stringResource(R.string.missions_open)) }
        when {
            modules == null -> Unit
            modules.isEmpty() -> Text(stringResource(R.string.content_unavailable), style = MaterialTheme.typography.bodyLarge)
            else -> modules.forEach { ModuleCard(it, onOpenModule) }
        }
    }
}

@Composable
fun ModuleCard(progress: ModuleProgress, onOpenModule: (String) -> Unit) {
    val module = progress.module
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(module.title, style = MaterialTheme.typography.titleMedium)
            Text(module.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.module_lessons_progress, progress.completedLessons, progress.lessons.size),
                style = MaterialTheme.typography.labelLarge,
            )
            if (module.assessment != null) AssessmentStatus(progress.bestAssessmentScore)
            if (module.reviewStatus == ReviewStatus.DRAFT) DraftBanner()
            Button(onClick = { onOpenModule(module.id) }) { Text(stringResource(R.string.module_open)) }
        }
    }
}

@Composable
private fun AssessmentStatus(best: Double?) = Text(
    if (best == null) stringResource(R.string.module_assessment_not_done)
    else stringResource(R.string.module_best_score, (best * 100).roundToInt()),
    style = MaterialTheme.typography.labelLarge,
)

/** Detalle de un módulo: lecciones en el orden recomendado y evaluación final. */
@Composable
fun ModuleRoute(
    moduleId: String,
    content: ContentRepository,
    progress: ProgressRepository,
    onOpenLesson: (String) -> Unit,
    onExit: () -> Unit,
) {
    val viewModel: ModuleViewModel = viewModel(key = "module-$moduleId", factory = ModuleViewModel.factory(moduleId, content, progress))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ModuleScreen(state, onOpenLesson, onExit)
}

@Composable
fun ModuleScreen(state: ModuleUiState, onOpenLesson: (String) -> Unit, onExit: () -> Unit, modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        when (state) {
            ModuleUiState.Loading -> Text(stringResource(R.string.lesson_loading))
            ModuleUiState.NotFound, ModuleUiState.InvalidContent -> {
                Text(stringResource(R.string.module_unavailable), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onExit) { Text(stringResource(R.string.lesson_back_home)) }
            }
            is ModuleUiState.Loaded -> ModuleContent(state.progress, onOpenLesson)
        }
    }
}

@Composable
private fun ModuleContent(progress: ModuleProgress, onOpenLesson: (String) -> Unit) {
    val module = progress.module
    Heading(module.title, large = true)
    if (module.reviewStatus == ReviewStatus.DRAFT) DraftBanner()
    Text(module.description, style = MaterialTheme.typography.bodyLarge)
    Heading(stringResource(R.string.module_lessons_title))
    Text(stringResource(R.string.module_order_note), style = MaterialTheme.typography.bodyMedium)
    progress.lessons.forEachIndexed { i, lesson -> LessonRow(i + 1, lesson, onOpenLesson) }
    module.assessment?.let { assessment ->
        Heading(stringResource(R.string.module_assessment_title))
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(assessment.title, style = MaterialTheme.typography.titleMedium)
                Text(assessment.description, style = MaterialTheme.typography.bodyMedium)
                AssessmentStatus(progress.bestAssessmentScore)
                Button(onClick = { onOpenLesson(assessment.id) }) {
                    Text(
                        stringResource(
                            if (progress.bestAssessmentScore == null) R.string.module_assessment_start else R.string.module_assessment_retake,
                        ),
                    )
                }
            }
        }
    }
    progress.challenge?.let { ChallengeCard(it, onOpenLesson) }
}

@Composable
private fun ChallengeCard(challenge: ChallengeProgress, onOpenLesson: (String) -> Unit) {
    Heading(stringResource(R.string.module_challenge_title))
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(challenge.challenge.title, style = MaterialTheme.typography.titleMedium)
            Text(challenge.challenge.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                when {
                    // Cerrado: se explica qué falta, sin botón que no haga nada.
                    !challenge.unlocked -> stringResource(R.string.challenge_locked, challenge.lessonsRemaining)
                    challenge.completed -> stringResource(R.string.module_challenge_done)
                    else -> stringResource(R.string.module_challenge_progress, challenge.solvedSteps, challenge.totalSteps)
                },
                style = MaterialTheme.typography.labelLarge,
            )
            if (challenge.unlocked) {
                Button(onClick = { onOpenLesson(challenge.challenge.id) }) {
                    Text(stringResource(if (challenge.completed) R.string.module_challenge_repeat else R.string.challenge_start))
                }
            }
        }
    }
}

@Composable
private fun LessonRow(number: Int, progress: LessonProgress, onOpenLesson: (String) -> Unit) {
    // La tarjeta entera es el botón: TalkBack lee el título y el estado juntos.
    NeonCard(onClick = { onOpenLesson(progress.lesson.id) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.module_lesson_title, number, progress.lesson.title), style = MaterialTheme.typography.titleSmall)
            Text(
                if (progress.completed) stringResource(R.string.module_lesson_done)
                else stringResource(R.string.module_lesson_progress, progress.solvedExercises, progress.totalExercises),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
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
