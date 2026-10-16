package com.funcode.academy.ui.rpg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.funcode.academy.R
import com.funcode.academy.domain.rpg.LevelProgress
import com.funcode.academy.ui.components.NeonBadge
import com.funcode.academy.ui.components.NeonCard
import com.funcode.academy.ui.components.NeonProgressBar

/** Nivel, XP total y barra de progreso hacia el siguiente nivel (todo calculado desde el registro de XP). */
@Composable
fun LevelProgressCard(progress: LevelProgress, modifier: Modifier = Modifier) {
    NeonCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // La insignia repite el número de nivel de forma visual; TalkBack lee el texto de al lado.
                NeonBadge(progress.level.toString())
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.level_title, progress.level), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.home_total_xp, progress.totalXp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            NeonProgressBar(
                fraction = progress.fraction,
                description = stringResource(
                    R.string.level_progress_description,
                    progress.level + 1,
                    progress.xpIntoLevel,
                    progress.xpForNextLevel,
                ),
            )
            Text(
                stringResource(R.string.level_remaining, progress.xpRemaining, progress.level + 1),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
