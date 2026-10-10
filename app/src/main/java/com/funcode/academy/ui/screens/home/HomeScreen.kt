package com.funcode.academy.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/** Conecta la pantalla de Inicio con su ViewModel. */
@Composable
fun HomeRoute(progressRepository: ProgressRepository) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(progressRepository))
    val totalXp by viewModel.totalXp.collectAsStateWithLifecycle()
    HomeScreen(totalXp = totalXp)
}

/** Pantalla de Inicio. Recibe el estado ya calculado, por eso es fácil de probar. */
@Composable
fun HomeScreen(totalXp: Int?, modifier: Modifier = Modifier) {
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
    }
}
