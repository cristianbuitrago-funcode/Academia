package com.funcode.academy.ui.screens.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.funcode.academy.domain.progress.ProgressRepository
import com.funcode.academy.domain.progress.XpSourceType
import com.funcode.academy.ui.rpg.LevelProgressCard
import com.funcode.academy.ui.rpg.descriptionRes
import com.funcode.academy.ui.rpg.titleRes
import java.text.DateFormat
import java.util.Date

@Composable
fun ProfileRoute(progressRepository: ProgressRepository) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(progressRepository))
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileScreen(state)
}

/** Perfil: nivel, resumen de recompensas y logros. Las misiones tienen su propia pantalla. */
@Composable
fun ProfileScreen(state: ProfileState?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Heading(stringResource(R.string.profile_title), large = true)
        if (state != null) {
            LevelProgressCard(state.level)

            Heading(stringResource(R.string.profile_rewards_title))
            if (state.rewards.isEmpty()) {
                Text(stringResource(R.string.profile_no_rewards), style = MaterialTheme.typography.bodyLarge)
            } else {
                RewardLine(R.string.profile_rewards_exercises, state.rewards[XpSourceType.EXERCISE])
                RewardLine(R.string.profile_rewards_lessons, state.rewards[XpSourceType.LESSON])
                RewardLine(R.string.profile_rewards_assessments, state.rewards[XpSourceType.ASSESSMENT])
                RewardLine(R.string.profile_rewards_challenges, state.rewards[XpSourceType.CHALLENGE])
                RewardLine(R.string.profile_rewards_missions, state.rewards[XpSourceType.MISSION])
            }

            Heading(stringResource(R.string.profile_achievements_title, state.unlockedCount, state.achievements.size))
            state.achievements.forEach { AchievementCard(it) }
        }
    }
}

@Composable
private fun RewardLine(@StringRes label: Int, totals: RewardTotals?) {
    val value = totals ?: RewardTotals(0, 0)
    Text(stringResource(label, value.count, value.xp), style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun AchievementCard(status: AchievementStatus) {
    val unlocked = status.unlockedAt != null
    Card(
        colors = if (unlocked) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else CardDefaults.cardColors(),
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(status.achievement.titleRes), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(status.achievement.descriptionRes), style = MaterialTheme.typography.bodyMedium)
            Text(
                status.unlockedAt?.let { stringResource(R.string.achievement_unlocked_on, DateFormat.getDateInstance().format(Date(it))) }
                    ?: stringResource(R.string.achievement_locked),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun Heading(text: String, large: Boolean = false) = Text(
    text = text,
    style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
    modifier = Modifier.semantics { heading() },
)
