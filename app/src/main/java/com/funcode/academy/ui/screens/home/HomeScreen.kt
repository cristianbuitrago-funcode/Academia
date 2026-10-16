package com.funcode.academy.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funcode.academy.R
import com.funcode.academy.ui.components.NeonCard
import com.funcode.academy.ui.components.SectionTitle
import com.funcode.academy.ui.components.neonBrush
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.learning.Recommendation
import com.funcode.academy.domain.mission.MissionProgress
import com.funcode.academy.domain.mission.MissionStatus
import com.funcode.academy.domain.progress.ModuleProgress
import com.funcode.academy.domain.progress.NextActivity
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.rpg.LevelProgress
import com.funcode.academy.domain.rpg.RewardEngine
import com.funcode.academy.ui.rpg.LevelProgressCard

/** Conecta la pantalla de Inicio con su ViewModel. */
@Composable
fun HomeRoute(
    progressRepository: ProgressRepository,
    contentRepository: ContentRepository,
    rewardEngine: RewardEngine,
    onOpenLesson: (String) -> Unit,
    onOpenModule: (String) -> Unit,
    onOpenMissions: () -> Unit,
    onOpenMission: (String) -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(progressRepository, contentRepository, rewardEngine))
    val level by viewModel.level.collectAsStateWithLifecycle()
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    val missions by viewModel.missions.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    HomeScreen(
        level = level,
        modules = modules,
        missions = missions,
        recommendations = recommendations,
        onOpenLesson = onOpenLesson,
        onOpenModule = onOpenModule,
        onOpenMissions = onOpenMissions,
        onOpenMission = onOpenMission,
    )
}

/** Pantalla de Inicio. Recibe el estado ya calculado, por eso es fácil de probar. */
@Composable
fun HomeScreen(
    level: LevelProgress?,
    modules: List<ModuleProgress>?,
    missions: List<MissionProgress>?,
    recommendations: List<Recommendation>,
    onOpenLesson: (String) -> Unit,
    onOpenModule: (String) -> Unit,
    onOpenMissions: () -> Unit,
    onOpenMission: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        HeroHeader()
        level?.let { LevelProgressCard(it) }
        Text(
            text = stringResource(R.string.home_xp_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle(stringResource(R.string.home_continue_title))
        when {
            modules == null -> Unit
            modules.isEmpty() -> Text(stringResource(R.string.content_unavailable), style = MaterialTheme.typography.bodyLarge)
            // Se continúa por el primer módulo que no esté terminado.
            else -> ContinueCard(modules.firstOrNull { it.next != NextActivity.ModuleDone } ?: modules.first(), onOpenLesson, onOpenModule)
        }
        if (recommendations.isNotEmpty()) RecommendationsCard(recommendations, onOpenLesson)
        if (!missions.isNullOrEmpty()) MissionHomeCard(missions, onOpenMissions, onOpenMission)
    }
}

/** Recomendaciones de repaso: solo con resultados reales; nunca bloquean ni dan XP. */
@Composable
private fun RecommendationsCard(recommendations: List<Recommendation>, onOpenLesson: (String) -> Unit) {
    SectionTitle(stringResource(R.string.home_recommendations_title))
    Text(stringResource(R.string.home_recommendations_note), style = MaterialTheme.typography.bodyMedium)
    recommendations.take(MAX_RECOMMENDATIONS).forEach { r ->
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (r) {
                    is Recommendation.ResumeLesson -> {
                        Text(stringResource(R.string.recommend_resume, r.lesson.title, r.solved, r.total), style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { onOpenLesson(r.lesson.id) }) { Text(stringResource(R.string.recommend_resume_button)) }
                    }
                    is Recommendation.ReviewLesson -> {
                        Text(stringResource(R.string.recommend_review, r.lesson.title, r.struggledExercises), style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { onOpenLesson(r.lesson.id) }) { Text(stringResource(R.string.recommend_review_button)) }
                    }
                    is Recommendation.RetryAssessment -> {
                        Text(
                            stringResource(R.string.recommend_retry, r.assessment.title, r.bestPercent, r.thresholdPercent),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (r.lessonsToReview.isNotEmpty()) {
                            Text(
                                stringResource(R.string.recommend_retry_review_first, r.lessonsToReview.joinToString { "«${it.title}»" }),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Button(onClick = { onOpenLesson(r.assessment.id) }) { Text(stringResource(R.string.recommend_retry_button)) }
                    }
                }
            }
        }
    }
}

private const val MAX_RECOMMENDATIONS = 3

/** Misión actual: la primera en curso o, si no hay, la primera disponible. */
@Composable
private fun MissionHomeCard(missions: List<MissionProgress>, onOpenMissions: () -> Unit, onOpenMission: (String) -> Unit) {
    val current = missions.firstOrNull { it.status == MissionStatus.IN_PROGRESS }
        ?: missions.firstOrNull { it.status == MissionStatus.AVAILABLE }
    SectionTitle(stringResource(R.string.home_missions_title))
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.home_missions_progress, missions.count { it.status == MissionStatus.COMPLETED }, missions.size),
                style = MaterialTheme.typography.labelLarge,
            )
            if (current != null) {
                Text(stringResource(R.string.home_current_mission, current.mission.title), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { onOpenMission(current.mission.id) }) { Text(stringResource(R.string.home_open_mission)) }
            } else {
                Text(stringResource(R.string.home_missions_done), style = MaterialTheme.typography.bodyLarge)
            }
            OutlinedButton(onClick = onOpenMissions) { Text(stringResource(R.string.missions_open)) }
        }
    }
}

/** Cabecera de bienvenida: etiqueta «terminal», título y presentación sobre un panel de neón. */
@Composable
private fun HeroHeader() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.surfaceContainer, colors.secondaryContainer)))
            .border(1.dp, neonBrush(), MaterialTheme.shapes.large)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.home_hero_tag),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            color = colors.onSecondaryContainer,
            // Decorativa: el título siguiente ya presenta la pantalla.
            modifier = Modifier.clearAndSetSemantics { },
        )
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = stringResource(R.string.home_intro), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
    }
}

@Composable
private fun ContinueCard(progress: ModuleProgress, onOpenLesson: (String) -> Unit, onOpenModule: (String) -> Unit) {
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(progress.module.title, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.module_lessons_progress, progress.completedLessons, progress.lessons.size),
                style = MaterialTheme.typography.labelLarge,
            )
            when (val next = progress.next) {
                is NextActivity.Lesson -> {
                    Text(stringResource(R.string.home_next_lesson, next.lesson.title), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { onOpenLesson(next.lesson.id) }) { Text(stringResource(R.string.home_continue_button)) }
                }
                is NextActivity.Assessment -> {
                    Text(stringResource(R.string.home_next_assessment, next.assessment.title), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { onOpenLesson(next.assessment.id) }) { Text(stringResource(R.string.home_continue_button)) }
                }
                is NextActivity.Challenge -> {
                    Text(stringResource(R.string.home_next_challenge, next.challenge.title), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { onOpenLesson(next.challenge.id) }) { Text(stringResource(R.string.home_continue_button)) }
                }
                NextActivity.ModuleDone -> Text(stringResource(R.string.home_module_done), style = MaterialTheme.typography.bodyLarge)
            }
            OutlinedButton(onClick = { onOpenModule(progress.module.id) }) { Text(stringResource(R.string.home_view_module)) }
        }
    }
}
