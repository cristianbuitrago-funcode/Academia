package com.funcode.academy.ui.rpg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.funcode.academy.R
import com.funcode.academy.ui.components.NeonCard
import com.funcode.academy.ui.screens.lesson.RewardNotice

/** Recompensas recién obtenidas. TalkBack las anuncia al aparecer. */
@Composable
fun RewardNoticeCard(notice: RewardNotice, modifier: Modifier = Modifier) {
    NeonCard(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (notice.xpGained > 0) {
                Text(stringResource(R.string.reward_xp_gained, notice.xpGained), style = MaterialTheme.typography.titleMedium)
            } else if (notice.alreadyRewarded) {
                Text(stringResource(R.string.reward_already_rewarded), style = MaterialTheme.typography.bodyMedium)
            }
            if (notice.lessonCompleted) Text(stringResource(R.string.reward_lesson_completed), style = MaterialTheme.typography.bodyLarge)
            if (notice.assessmentPassed) Text(stringResource(R.string.reward_assessment_passed), style = MaterialTheme.typography.bodyLarge)
            if (notice.challengeCompleted) Text(stringResource(R.string.reward_challenge_completed), style = MaterialTheme.typography.bodyLarge)
            notice.completedMissions.forEach {
                Text(stringResource(R.string.reward_mission_completed, it), style = MaterialTheme.typography.bodyLarge)
            }
            notice.newLevel?.let { Text(stringResource(R.string.reward_level_up, it), style = MaterialTheme.typography.titleMedium) }
            notice.achievements.forEach {
                Text(stringResource(R.string.reward_achievement, stringResource(it.titleRes)), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
