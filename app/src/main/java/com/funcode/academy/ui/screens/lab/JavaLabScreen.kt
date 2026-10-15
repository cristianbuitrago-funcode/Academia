package com.funcode.academy.ui.screens.lab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.funcode.academy.R
import com.funcode.academy.domain.runner.CodeRunner
import com.funcode.academy.domain.runner.Limit
import com.funcode.academy.domain.runner.ProgramEvaluation
import com.funcode.academy.domain.runner.RejectReason
import com.funcode.academy.domain.runner.RunResult
import com.funcode.academy.ui.components.CodeBlock
import com.funcode.academy.ui.components.NeonCard
import com.funcode.academy.ui.components.SectionTitle

@Composable
fun JavaLabRoute(runner: CodeRunner) {
    val viewModel: JavaLabViewModel = viewModel(factory = JavaLabViewModel.factory(runner))
    val state by viewModel.state.collectAsStateWithLifecycle()
    JavaLabScreen(state, viewModel::onCodeChange, viewModel::onRun, viewModel::onCancel, viewModel::onReset)
}

@Composable
fun JavaLabScreen(
    state: JavaLabViewModel.UiState,
    onCodeChange: (String) -> Unit,
    onRun: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.lab_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.lab_prototype_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionTitle(stringResource(R.string.lab_task_title))
        Text(stringResource(R.string.lab_task), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = state.code,
            onValueChange = onCodeChange,
            label = { Text(stringResource(R.string.lab_editor_label)) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
            ),
            enabled = !state.running,
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
        )
        Text(
            stringResource(R.string.lab_chars, state.code.length, 10_000),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.running) {
                OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.lab_cancel)) }
            } else {
                Button(onClick = onRun) { Text(stringResource(R.string.lab_run)) }
                OutlinedButton(onClick = onReset) { Text(stringResource(R.string.lab_reset)) }
            }
        }
        if (state.running) {
            Text(stringResource(R.string.lab_running), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.result?.let { ResultPanel(it, state.outcome) }
    }
}

@Composable
private fun ResultPanel(result: RunResult, outcome: ProgramEvaluation.Outcome?) {
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Cada mensaje dice qué ocurrió de verdad: solo se habla de «ejecutado» si hubo ejecución.
            val status = when (result) {
                is RunResult.Completed -> stringResource(R.string.lab_status_completed)
                is RunResult.RuntimeError -> stringResource(R.string.lab_status_runtime_error, result.exception, result.line?.toString() ?: "?")
                is RunResult.CompilationFailed -> stringResource(R.string.lab_status_compile_error)
                is RunResult.LimitExceeded -> when (result.limit) {
                    Limit.TIME -> stringResource(R.string.lab_status_time_limit)
                    Limit.OUTPUT -> stringResource(R.string.lab_status_output_limit)
                    Limit.MEMORY -> stringResource(R.string.lab_status_memory_limit)
                }
                is RunResult.Rejected -> when (result.reason) {
                    RejectReason.EMPTY_SOURCE -> stringResource(R.string.lab_status_empty)
                    RejectReason.SOURCE_TOO_LARGE -> stringResource(R.string.lab_status_too_large)
                }
                is RunResult.Cancelled -> stringResource(R.string.lab_status_cancelled)
                is RunResult.Unavailable -> stringResource(R.string.lab_status_unavailable, result.reason)
            }
            Text(status, style = MaterialTheme.typography.titleMedium)
            if (result is RunResult.CompilationFailed) {
                result.diagnostics.forEach { d ->
                    Text(
                        if (d.line != null) stringResource(R.string.lab_diagnostic_line, d.line!!, d.message) else d.message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (result is RunResult.RuntimeError && result.message != null) {
                Text(result.message!!, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.error)
            }
            val stdout = when (result) {
                is RunResult.Completed -> result.stdout
                is RunResult.RuntimeError -> result.stdout
                is RunResult.LimitExceeded -> result.stdout
                else -> null
            }
            if (stdout != null) {
                Text(stringResource(R.string.lab_output_title), style = MaterialTheme.typography.labelLarge)
                if (stdout.isEmpty()) Text(stringResource(R.string.lab_output_empty), style = MaterialTheme.typography.bodyMedium)
                else CodeBlock(stdout.trimEnd('\n'), showLineNumbers = false)
            }
            when (outcome) {
                ProgramEvaluation.Outcome.OutputMatches -> Text(stringResource(R.string.lab_check_ok), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                ProgramEvaluation.Outcome.OutputDiffers -> Text(stringResource(R.string.lab_check_differs), style = MaterialTheme.typography.bodyLarge)
                else -> Unit
            }
            result.timings.totalMs?.let {
                Text(stringResource(R.string.lab_timing, it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
