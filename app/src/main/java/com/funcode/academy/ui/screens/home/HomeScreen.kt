package com.funcode.academy.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.LessonSummary
import com.funcode.academy.domain.lesson.ReviewStatus
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.ui.components.DraftBanner

/** Conecta la pantalla de Inicio con su ViewModel. */
@Composable
fun HomeRoute(
    progressRepository: ProgressRepository,
    contentRepository: ContentRepository,
    onOpenLesson: (String) -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(progressRepository, contentRepository))
    val totalXp by viewModel.totalXp.collectAsStateWithLifecycle()
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()
    HomeScreen(totalXp = totalXp, lessons = lessons, onOpenLesson = onOpenLesson)
}

/** Pantalla de Inicio. Recibe el estado ya calculado, por eso es fácil de probar. */
@Composable
fun HomeScreen(
    totalXp: Int?,
    lessons: List<LessonSummary>?,
    onOpenLesson: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = stringResource(R.string.home_intro), style = MaterialTheme.typography.bodyLarge)
        if (totalXp != null) {
            Text(
                text = stringResource(R.string.home_total_xp, totalXp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(text = stringResource(R.string.home_xp_explanation), style = MaterialTheme.typography.bodyMedium)

        Text(
            text = stringResource(R.string.home_lessons_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        when {
            lessons == null -> Unit
            lessons.isEmpty() -> Text(stringResource(R.string.home_no_lessons), style = MaterialTheme.typography.bodyLarge)
            else -> lessons.forEach { LessonCard(it, onOpenLesson) }
        }
    }
}

@Composable
private fun LessonCard(lesson: LessonSummary, onOpenLesson: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(lesson.title, style = MaterialTheme.typography.titleMedium)
            Text(lesson.description, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.home_lesson_exercises, lesson.exerciseCount), style = MaterialTheme.typography.labelLarge)
            if (lesson.reviewStatus == ReviewStatus.DRAFT) DraftBanner()
            Button(onClick = { onOpenLesson(lesson.id) }) { Text(stringResource(R.string.home_open_lesson)) }
        }
    }
}
