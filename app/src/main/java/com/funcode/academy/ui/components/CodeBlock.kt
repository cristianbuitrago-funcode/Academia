package com.funcode.academy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.funcode.academy.R

/**
 * Bloque de código Java en fuente monoespaciada, con números de línea y
 * desplazamiento horizontal para que las líneas largas no se corten.
 */
@Composable
private fun lineDescription(number: Int, line: String) = stringResource(R.string.code_line_description, number, line.trim())

@Composable
fun CodeBlock(code: String, modifier: Modifier = Modifier, showLineNumbers: Boolean = true) {
    val lines = code.lines()
    val width = lines.size.toString().length
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        lines.forEachIndexed { i, line ->
            val prefix = if (showLineNumbers) "${(i + 1).toString().padStart(width)}  " else ""
            Text(
                text = prefix + line,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = false,
                // TalkBack lee «Línea 3: …» en lugar del número pegado al código y los espacios de alineación.
                modifier = if (showLineNumbers) Modifier.semantics { contentDescription = lineDescription(i + 1, line) } else Modifier,
            )
        }
    }
}
