package com.funcode.academy.ui.rpg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.funcode.academy.R
import com.funcode.academy.domain.rpg.LevelProgress

/** Nivel, XP total y barra de progreso hacia el siguiente nivel (todo calculado desde el registro de XP). */
@Composable
fun LevelProgressCard(progress: LevelProgress, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.level_title, progress.level), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.home_total_xp, progress.totalXp), style = MaterialTheme.typography.titleMedium)
            val description = stringResource(
                R.string.level_progress_description,
                progress.level + 1,
                progress.xpIntoLevel,
                progress.xpForNextLevel,
            )
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = description },
            )
            Text(
                stringResource(R.string.level_remaining, progress.xpRemaining, progress.level + 1),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
