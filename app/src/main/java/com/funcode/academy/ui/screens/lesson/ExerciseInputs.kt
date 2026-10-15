package com.funcode.academy.ui.screens.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.funcode.academy.R
import com.funcode.academy.domain.lesson.Answer
import com.funcode.academy.domain.lesson.ExerciseContent
import com.funcode.academy.domain.lesson.LessonParser
import com.funcode.academy.ui.components.CodeBlock

/**
 * Controles de respuesta de cada tipo de ejercicio. Solo construyen la respuesta
 * ([Answer]); la comprobación la hace el dominio.
 */
@Composable
fun ExerciseInput(content: ExerciseContent, draft: Answer, enabled: Boolean, onDraftChange: (Answer) -> Unit) {
    when (content) {
        is ExerciseContent.MultipleChoice -> MultipleChoiceInput(content, draft as Answer.Choice, enabled, onDraftChange)
        is ExerciseContent.TrueFalse -> TrueFalseInput(content, draft as Answer.TrueFalse, enabled, onDraftChange)
        is ExerciseContent.FillInBlanks -> FillInBlanksInput(content, draft as Answer.Blanks, enabled, onDraftChange)
        is ExerciseContent.OrderLines -> OrderLinesInput(content, draft as Answer.Order, enabled, onDraftChange)
        is ExerciseContent.PredictOutput -> PredictOutputInput(content, draft as Answer.Output, enabled, onDraftChange)
        is ExerciseContent.FindError -> FindErrorInput(content, draft as Answer.ErrorLines, enabled, onDraftChange)
        is ExerciseContent.MatchPairs -> MatchPairsInput(content, draft as Answer.Pairs, enabled, onDraftChange)
    }
}

@Composable
private fun OptionRow(
    text: String,
    selected: Boolean,
    multiple: Boolean,
    enabled: Boolean,
    monospace: Boolean = false,
    onClick: () -> Unit,
) {
    val role = if (multiple) Role.Checkbox else Role.RadioButton
    val base = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    val modifier = if (multiple) {
        base.toggleable(value = selected, enabled = enabled, role = role, onValueChange = { onClick() })
    } else {
        base.selectable(selected = selected, enabled = enabled, role = role, onClick = onClick)
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (multiple) Checkbox(checked = selected, onCheckedChange = null, enabled = enabled)
        else RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = if (monospace) FontFamily.Monospace else null,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun MultipleChoiceInput(content: ExerciseContent.MultipleChoice, draft: Answer.Choice, enabled: Boolean, onDraftChange: (Answer) -> Unit) {
    val multiple = content.allowsMultipleSelection
    Column(Modifier.selectableGroup()) {
        if (multiple) Text(stringResource(R.string.choice_select_all), style = MaterialTheme.typography.bodyMedium)
        content.options.forEachIndexed { i, option ->
            val selected = i in draft.selected
            OptionRow(option.text, selected, multiple, enabled, monospace = true, onClick = {
                val newSelection = when {
                    !multiple -> setOf(i)
                    selected -> draft.selected - i
                    else -> draft.selected + i
                }
                onDraftChange(Answer.Choice(newSelection))
            })
        }
    }
}

@Composable
private fun TrueFalseInput(content: ExerciseContent.TrueFalse, draft: Answer.TrueFalse, enabled: Boolean, onDraftChange: (Answer) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(content.statement, style = MaterialTheme.typography.bodyLarge)
        OptionRow(stringResource(R.string.answer_true), draft.value == true, multiple = false, enabled = enabled) {
            onDraftChange(Answer.TrueFalse(true))
        }
        OptionRow(stringResource(R.string.answer_false), draft.value == false, multiple = false, enabled = enabled) {
            onDraftChange(Answer.TrueFalse(false))
        }
    }
}

@Composable
private fun FillInBlanksInput(content: ExerciseContent.FillInBlanks, draft: Answer.Blanks, enabled: Boolean, onDraftChange: (Answer) -> Unit) {
    // Los marcadores {{n}} se muestran como [n+1], el número del hueco.
    val shown = LessonParser.BLANK_MARKER.replace(content.template) { "[${it.groupValues[1].toInt() + 1}]" }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CodeBlock(shown)
        draft.values.forEachIndexed { i, value ->
            OutlinedTextField(
                value = value,
                onValueChange = { new -> onDraftChange(Answer.Blanks(draft.values.toMutableList().also { it[i] = new })) },
                label = { Text(stringResource(R.string.blank_label, i + 1)) },
                singleLine = true,
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OrderLinesInput(content: ExerciseContent.OrderLines, draft: Answer.Order, enabled: Boolean, onDraftChange: (Answer) -> Unit) {
    val order = draft.lineIndices
    fun move(from: Int, to: Int) {
        val list = order.toMutableList()
        list.add(to, list.removeAt(from))
        onDraftChange(Answer.Order(list))
    }
    Column {
        Text(stringResource(R.string.order_instructions), style = MaterialTheme.typography.bodyMedium)
        order.forEachIndexed { position, lineIndex ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${position + 1}. ${content.lines[lineIndex]}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                val upLabel = stringResource(R.string.order_move_up, position + 1)
                IconButton(
                    onClick = { move(position, position - 1) },
                    enabled = enabled && position > 0,
                    modifier = Modifier.semantics { contentDescription = upLabel },
                ) { Icon(painterResource(R.drawable.ic_arrow_up), contentDescription = null) }
                val downLabel = stringResource(R.string.order_move_down, position + 1)
                IconButton(
                    onClick = { move(position, position + 1) },
                    enabled = enabled && position < order.lastIndex,
                    modifier = Modifier.semantics { contentDescription = downLabel },
                ) { Icon(painterResource(R.drawable.ic_arrow_down), contentDescription = null) }
            }
        }
    }
}

@Composable
private fun PredictOutputInput(content: ExerciseContent.PredictOutput, draft: Answer.Output, enabled: Boolean, onDraftChange: (Answer) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CodeBlock(content.code)
        OutlinedTextField(
            value = draft.text,
            onValueChange = { onDraftChange(Answer.Output(it)) },
            label = { Text(stringResource(R.string.output_label)) },
            enabled = enabled,
            minLines = 3,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FindErrorInput(content: ExerciseContent.FindError, draft: Answer.ErrorLines, enabled: Boolean, onDraftChange: (Answer) -> Unit) {
    Column {
        Text(stringResource(R.string.find_error_instructions), style = MaterialTheme.typography.bodyMedium)
        content.lines.forEachIndexed { i, line ->
            val number = i + 1
            val selected = number in draft.lines
            OptionRow(
                text = stringResource(R.string.find_error_line, number, line.trim()),
                selected = selected,
                multiple = true,
                enabled = enabled,
                monospace = true,
            ) { onDraftChange(Answer.ErrorLines(if (selected) draft.lines - number else draft.lines + number)) }
        }
    }
}

@Composable
private fun MatchPairsInput(content: ExerciseContent.MatchPairs, draft: Answer.Pairs, enabled: Boolean, onDraftChange: (Answer) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        content.left.forEachIndexed { l, concept ->
            Column {
                Text(concept, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge)
                var expanded by remember { mutableStateOf(false) }
                val chosen = draft.pairs[l]?.let { content.right[it] }
                val pickLabel = stringResource(R.string.match_pick_for, concept)
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$pickLabel. ${chosen ?: ""}" },
                    ) { Text(chosen ?: stringResource(R.string.match_choose)) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        content.right.forEachIndexed { r, description ->
                            DropdownMenuItem(
                                text = { Text(description) },
                                onClick = {
                                    expanded = false
                                    onDraftChange(Answer.Pairs(draft.pairs + (l to r)))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
