package com.funcode.academy.ui.screens.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funcode.academy.R
import com.funcode.academy.domain.lesson.Answer
import com.funcode.academy.domain.lesson.AnswerResult
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.IncompleteReason
import com.funcode.academy.domain.lesson.Lesson
import com.funcode.academy.domain.lesson.LessonKind
import com.funcode.academy.domain.lesson.ReviewStatus
import com.funcode.academy.domain.lesson.Section
import com.funcode.academy.domain.lesson.SessionSummary
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.rpg.RewardEngine
import com.funcode.academy.domain.settings.SettingsRepository
import com.funcode.academy.ui.components.CodeBlock
import com.funcode.academy.ui.components.DraftBanner
import com.funcode.academy.ui.rpg.RewardNoticeCard
import kotlin.math.roundToInt

@Composable
fun LessonRoute(
    lessonId: String,
    contentRepository: ContentRepository,
    progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository,
    rewardEngine: RewardEngine,
    onExit: () -> Unit,
) {
    val viewModel: LessonViewModel = viewModel(
        key = "lesson-$lessonId",
        factory = LessonViewModel.factory(lessonId, contentRepository, progressRepository, settingsRepository, rewardEngine),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rewardNotice by viewModel.rewardNotice.collectAsStateWithLifecycle()
    val sessionXp by viewModel.sessionXp.collectAsStateWithLifecycle()
    LessonScreen(
        state = state,
        rewardNotice = rewardNotice,
        sessionXp = sessionXp,
        onStart = viewModel::startExercises,
        onDraftChange = viewModel::updateDraft,
        onCheck = viewModel::check,
        onHint = viewModel::revealHint,
        onRetry = viewModel::retry,
        onNext = viewModel::next,
        onExit = onExit,
    )
}

@Composable
fun LessonScreen(
    state: LessonUiState,
    rewardNotice: RewardNotice?,
    sessionXp: Int,
    onStart: () -> Unit,
    onDraftChange: (Answer) -> Unit,
    onCheck: () -> Unit,
    onHint: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (state) {
            LessonUiState.Loading -> Text(stringResource(R.string.lesson_loading))
            LessonUiState.NotFound -> Message(stringResource(R.string.lesson_not_found), onExit)
            LessonUiState.InvalidContent -> Message(stringResource(R.string.lesson_invalid), onExit)
            is LessonUiState.Intro -> Intro(state.lesson, state.sections, onStart)
            is LessonUiState.Locked -> {
                LessonHeader(state.lesson)
                Message(stringResource(R.string.challenge_locked, state.lessonsRemaining), onExit)
            }
            is LessonUiState.Exercising -> ExerciseStep(state, onDraftChange, onCheck, onHint, onRetry, onNext)
            is LessonUiState.Finished -> Summary(state.lesson, state.summary, sessionXp, onRestart = onStart, onExit = onExit)
        }
        if (state is LessonUiState.Exercising || state is LessonUiState.Finished) {
            rewardNotice?.let { RewardNoticeCard(it) }
        }
    }
}

@Composable
private fun Heading(text: String, large: Boolean = false) = Text(
    text = text,
    style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
    modifier = Modifier.semantics { heading() },
)

@Composable
private fun Message(text: String, onExit: () -> Unit) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
    Button(onClick = onExit) { Text(stringResource(R.string.lesson_back_home)) }
}

@Composable
private fun LessonHeader(lesson: Lesson) {
    Heading(lesson.title, large = true)
    if (lesson.reviewStatus == ReviewStatus.DRAFT) DraftBanner()
}

@Composable
private fun Intro(lesson: Lesson, sections: List<Section>, onStart: () -> Unit) {
    LessonHeader(lesson)
    Text(lesson.description, style = MaterialTheme.typography.bodyLarge)
    lesson.project?.let { project ->
        Heading(stringResource(R.string.project_situation_title))
        Text(project.situation, style = MaterialTheme.typography.bodyLarge)
        Heading(stringResource(R.string.project_prior_title))
        project.priorConcepts.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
    }
    Heading(stringResource(R.string.lesson_objectives_title))
    lesson.objectives.forEach { Text("• ${it.text}", style = MaterialTheme.typography.bodyLarge) }
    Heading(stringResource(R.string.lesson_theory_title))
    sections.forEach { section ->
        when (section) {
            is Section.Text -> Text(section.text, style = MaterialTheme.typography.bodyLarge)
            is Section.Callout -> Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(section.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(12.dp)) }
            is Section.Code -> {
                CodeBlock(section.code)
                if (section.lineComments.isNotEmpty()) {
                    Text(stringResource(R.string.lesson_code_comments_title), style = MaterialTheme.typography.titleSmall)
                    section.lineComments.forEach {
                        Text(stringResource(R.string.lesson_line_comment, it.line, it.comment), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
    if (lesson.kind == LessonKind.ASSESSMENT) Text(stringResource(R.string.assessment_rules), style = MaterialTheme.typography.titleSmall)
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(
                when (lesson.kind) {
                    LessonKind.LESSON -> R.string.lesson_start_exercises
                    LessonKind.ASSESSMENT -> R.string.lesson_start_assessment
                    LessonKind.CHALLENGE -> R.string.challenge_start
                },
            ),
        )
    }
}

@Composable
private fun ExerciseStep(
    state: LessonUiState.Exercising,
    onDraftChange: (Answer) -> Unit,
    onCheck: () -> Unit,
    onHint: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
) {
    val session = state.session
    val exercise = session.currentExercise
    val progress = session.currentProgress
    LessonHeader(session.lesson)
    Text(
        stringResource(
            when (session.lesson.kind) {
                LessonKind.LESSON -> R.string.lesson_exercise_progress
                LessonKind.ASSESSMENT -> R.string.assessment_progress
                LessonKind.CHALLENGE -> R.string.challenge_progress
            },
            session.currentIndex + 1,
            session.lesson.exercises.size,
        ),
        style = MaterialTheme.typography.labelLarge,
    )
    Text(stringResource(R.string.lesson_difficulty, exercise.difficulty), style = MaterialTheme.typography.labelMedium)
    Heading(exercise.prompt)

    // Tras una respuesta comprobada (correcta o incorrecta) los controles quedan bloqueados
    // hasta pulsar "Intentar de nuevo" o avanzar.
    val answered = state.feedback is AnswerResult.Correct || state.feedback is AnswerResult.Incorrect
    ExerciseInput(exercise.content, state.draft, enabled = !answered, onDraftChange = onDraftChange)

    exercise.hints.take(progress.hintsRevealed).forEachIndexed { i, hint ->
        Text(stringResource(R.string.lesson_hint, i + 1, hint), style = MaterialTheme.typography.bodyMedium)
    }

    // En una evaluación no se puede reintentar, así que tras un fallo también se explica la respuesta correcta.
    state.feedback?.let { Feedback(it, exercise.explanation, exercise.incorrectFeedback, explainOnIncorrect = !session.allowsRetries) }

    when (state.feedback) {
        // En una evaluación no hay reintentos: tras comprobar, solo se puede avanzar.
        AnswerResult.Correct, is AnswerResult.Incorrect -> if (!session.allowsRetries) {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (session.isLastExercise) R.string.assessment_see_result else R.string.assessment_next))
            }
        } else if (state.feedback == AnswerResult.Correct) {
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (session.isLastExercise) R.string.lesson_see_summary else R.string.lesson_next))
            }
        } else {
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.lesson_try_again)) }
            OutlinedButton(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (session.isLastExercise) R.string.lesson_see_summary else R.string.lesson_skip))
            }
        }
        else -> {
            Button(onClick = onCheck, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.lesson_check)) }
            if (progress.hintsRevealed < exercise.hints.size) {
                TextButton(onClick = onHint) {
                    Text(stringResource(R.string.lesson_show_hint, progress.hintsRevealed + 1, exercise.hints.size))
                }
            }
        }
    }
    Text(stringResource(R.string.feedback_validation_note), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun Feedback(result: AnswerResult, explanation: String, incorrectFeedback: String, explainOnIncorrect: Boolean) {
    val (container, content) = when (result) {
        AnswerResult.Correct -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        is AnswerResult.Incorrect -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        is AnswerResult.Incomplete -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        // liveRegion: TalkBack lee la retroalimentación en cuanto aparece.
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (result) {
                AnswerResult.Correct -> {
                    Text(stringResource(R.string.feedback_correct_title), style = MaterialTheme.typography.titleMedium)
                    Text(explanation, style = MaterialTheme.typography.bodyLarge)
                }
                is AnswerResult.Incorrect -> {
                    Text(stringResource(R.string.feedback_incorrect_title), style = MaterialTheme.typography.titleMedium)
                    result.optionFeedback?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    val correct = result.correctParts
                    val total = result.totalParts
                    if (correct != null && total != null) {
                        Text(stringResource(R.string.feedback_partial, correct, total), style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(incorrectFeedback, style = MaterialTheme.typography.bodyLarge)
                    if (explainOnIncorrect) Text(explanation, style = MaterialTheme.typography.bodyLarge)
                }
                is AnswerResult.Incomplete -> Text(stringResource(result.reason.message()), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun IncompleteReason.message(): Int = when (this) {
    IncompleteReason.NOTHING_SELECTED -> R.string.incomplete_nothing_selected
    IncompleteReason.EMPTY_BLANKS -> R.string.incomplete_empty_blanks
    IncompleteReason.EMPTY_OUTPUT -> R.string.incomplete_empty_output
    IncompleteReason.NOT_ALL_PAIRS -> R.string.incomplete_not_all_pairs
    IncompleteReason.DUPLICATED_PAIR -> R.string.incomplete_duplicated_pair
    IncompleteReason.INVALID_ORDER -> R.string.incomplete_invalid_order
}

@Composable
private fun Summary(lesson: Lesson, summary: SessionSummary, sessionXp: Int, onRestart: () -> Unit, onExit: () -> Unit) {
    LessonHeader(lesson)
    Heading(stringResource(R.string.summary_title))
    Text(stringResource(R.string.summary_total, summary.total), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(R.string.summary_first_try, summary.solvedFirstTry), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(R.string.summary_retries, summary.solved - summary.solvedFirstTry), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(R.string.summary_unsolved, summary.unsolved), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(R.string.summary_hints, summary.hintsUsed), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(R.string.summary_session_xp, sessionXp), style = MaterialTheme.typography.titleMedium)
    summary.outcomes.forEachIndexed { i, outcome ->
        Text(
            if (outcome.solved) stringResource(R.string.summary_exercise_solved, i + 1, outcome.attempts)
            else stringResource(R.string.summary_exercise_unsolved, i + 1),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    summary.assessmentScore?.let { score ->
        Text(
            stringResource(R.string.summary_assessment_score, score.correct, score.total, score.percent),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(stringResource(R.string.summary_assessment_saved), style = MaterialTheme.typography.bodyMedium)
    }
    val threshold = lesson.assessment?.passThreshold
    summary.assessmentPassed?.let { passed ->
        Text(
            if (passed || threshold == null) stringResource(R.string.summary_assessment_passed)
            else stringResource(R.string.summary_assessment_failed, (threshold * 100).roundToInt()),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    lesson.project?.let { project ->
        if (summary.unsolved == 0) {
            Heading(stringResource(R.string.project_recap_title))
            project.recap.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
        } else {
            Text(stringResource(R.string.project_unfinished, summary.unsolved), style = MaterialTheme.typography.bodyLarge)
        }
    }
    Text(stringResource(R.string.summary_note), style = MaterialTheme.typography.bodySmall)
    Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.summary_repeat)) }
    OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.lesson_back_home)) }
}
