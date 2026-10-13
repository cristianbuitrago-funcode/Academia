package com.funcode.academy.ui.screens.settings

import androidx.annotation.StringRes
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.funcode.academy.domain.settings.TextScale
import com.funcode.academy.domain.settings.ThemeMode

@Composable
fun SettingsRoute(settingsRepository: SettingsRepository) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(settingsRepository))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onAudienceSelected = viewModel::onAudienceSelected,
        onThemeSelected = viewModel::onThemeSelected,
        onTextScaleSelected = viewModel::onTextScaleSelected,
        onHintsChanged = viewModel::onHintsChanged,
        onResetPreferences = viewModel::onResetPreferences,
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAudienceSelected: (Audience) -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onTextScaleSelected: (TextScale) -> Unit,
    onHintsChanged: (Boolean) -> Unit,
    onResetPreferences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.settings_privacy_note), style = MaterialTheme.typography.bodyMedium)
        if (uiState !is SettingsUiState.Loaded) return@Column
        val prefs = uiState.preferences

        Section(R.string.settings_audience_title, R.string.settings_audience_description)
        if (prefs.audience == null) {
            Text(text = stringResource(R.string.settings_audience_not_chosen), style = MaterialTheme.typography.bodyMedium)
        }
        // selectableGroup: TalkBack anuncia las opciones como un grupo de botones de opción.
        Column(Modifier.selectableGroup()) {
            Audience.selectable.forEach { audience ->
                Option(stringResource(audience.labelRes()), selected = prefs.audience == audience) { onAudienceSelected(audience) }
            }
        }

        Section(R.string.settings_theme_title, R.string.settings_theme_description)
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                Option(stringResource(mode.labelRes()), selected = prefs.themeMode == mode) { onThemeSelected(mode) }
            }
        }

        Section(R.string.settings_text_title, R.string.settings_text_description)
        Column(Modifier.selectableGroup()) {
            TextScale.entries.forEach { scale ->
                Option(stringResource(scale.labelRes()), selected = prefs.textScale == scale) { onTextScaleSelected(scale) }
            }
        }

        Section(R.string.settings_help_title, R.string.settings_hints_description)
        // La fila entera es el interruptor (≥ 48 dp); TalkBack lo anuncia como interruptor con su estado.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = prefs.hintsEnabled, role = Role.Switch, onValueChange = onHintsChanged),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.settings_hints_label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = prefs.hintsEnabled, onCheckedChange = null)
        }

        Section(R.string.settings_reset_title, R.string.settings_reset_description)
        OutlinedButton(onClick = { confirmReset = true }) { Text(stringResource(R.string.settings_reset_button)) }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onResetPreferences()
                }) { Text(stringResource(R.string.settings_reset_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.settings_reset_cancel)) } },
        )
    }
}

@Composable
private fun Section(@StringRes title: Int, @StringRes description: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
    Text(text = stringResource(description), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Option(label: String, selected: Boolean, onClick: () -> Unit) {
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

@StringRes
private fun Audience.labelRes(): Int = when (this) {
    Audience.KIDS -> R.string.audience_kids
    Audience.TEENS -> R.string.audience_teens
    Audience.ADULTS -> R.string.audience_adults
}

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@StringRes
private fun TextScale.labelRes(): Int = when (this) {
    TextScale.NORMAL -> R.string.text_scale_normal
    TextScale.LARGE -> R.string.text_scale_large
    TextScale.EXTRA_LARGE -> R.string.text_scale_extra_large
}
