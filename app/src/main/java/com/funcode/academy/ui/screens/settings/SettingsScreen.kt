package com.funcode.academy.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funcode.academy.R
import com.funcode.academy.domain.model.Audience
import com.funcode.academy.domain.settings.SettingsRepository

@Composable
fun SettingsRoute(settingsRepository: SettingsRepository) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(settingsRepository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(uiState = uiState, onAudienceSelected = viewModel::onAudienceSelected)
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAudienceSelected: (Audience) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.settings_audience_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = stringResource(R.string.settings_audience_description), style = MaterialTheme.typography.bodyMedium)

        if (uiState is SettingsUiState.Loaded) {
            if (uiState.audience == null) {
                Text(text = stringResource(R.string.settings_audience_not_chosen), style = MaterialTheme.typography.bodyMedium)
            }
            // selectableGroup: TalkBack anuncia las opciones como un grupo de botones de opción.
            Column(Modifier.selectableGroup()) {
                Audience.selectable.forEach { audience ->
                    AudienceOption(
                        label = stringResource(audience.labelRes()),
                        selected = uiState.audience == audience,
                        onClick = { onAudienceSelected(audience) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AudienceOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // onClick = null: la fila entera es el objetivo táctil (≥ 48 dp).
        RadioButton(selected = selected, onClick = null)
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
    }
}

private fun Audience.labelRes(): Int = when (this) {
    Audience.TEENS -> R.string.audience_teens
    Audience.ADULTS -> R.string.audience_adults
    Audience.KIDS -> error("La ruta infantil no está disponible en esta versión.")
}
