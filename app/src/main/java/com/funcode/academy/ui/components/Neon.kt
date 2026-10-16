package com.funcode.academy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Piezas visuales de la «Academia futurista». Son solo decoración: no cambian el
// texto, el orden de lectura ni los botones que usa TalkBack.

/** Degradado de neón azul → verde de los bordes y acentos. */
@Composable
fun neonBrush(alpha: Float = 1f): Brush = Brush.linearGradient(
    listOf(MaterialTheme.colorScheme.primary.copy(alpha = alpha), MaterialTheme.colorScheme.secondary.copy(alpha = alpha)),
)

/**
 * Fondo de la app: degradado azul marino con una rejilla tenue y un halo de neón,
 * dibujados detrás del contenido (no se anuncian con TalkBack).
 */
@Composable
fun FuturisticBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    val grid = colors.primary.copy(alpha = 0.06f)
    val glowTop = colors.primary.copy(alpha = 0.18f)
    val glowBottom = colors.secondary.copy(alpha = 0.10f)
    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(colors.surfaceContainerLow, colors.background, colors.background)))
            .drawBehind {
                val step = 32.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += step
                }
                drawCircle(
                    Brush.radialGradient(listOf(glowTop, Color.Transparent), center = Offset(size.width, 0f), radius = size.width * 0.9f),
                    radius = size.width * 0.9f,
                    center = Offset(size.width, 0f),
                )
                drawCircle(
                    Brush.radialGradient(listOf(glowBottom, Color.Transparent), center = Offset(0f, size.height), radius = size.width),
                    radius = size.width,
                    center = Offset(0f, size.height),
                )
            },
        content = content,
    )
}

/**
 * Tarjeta con borde de neón. Mismo uso que `Card` de Material 3; si recibe [onClick]
 * es una tarjeta pulsable.
 */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = BorderStroke(1.dp, neonBrush(alpha = 0.55f))
    val shape = MaterialTheme.shapes.medium
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border, content = content)
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, border = border, content = content)
    }
}

/** Título de sección con una barra de neón a la izquierda. Se anuncia como encabezado. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(neonBrush()),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 12.dp).semantics { heading() },
        )
    }
}

/**
 * Barra de progreso de neón. Con [description], TalkBack la anuncia como barra de
 * progreso con ese texto; sin ella es decorativa (el avance ya se lee en otro texto).
 */
@Composable
fun NeonProgressBar(fraction: Float, modifier: Modifier = Modifier, description: String? = null) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val value = fraction.coerceIn(0f, 1f)
    val a11y = if (description == null) {
        Modifier.clearAndSetSemantics { }
    } else {
        Modifier.semantics {
            contentDescription = description
            progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
        }
    }
    Box(
        modifier
            .then(a11y)
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(value)
                .height(10.dp)
                .clip(CircleShape)
                .background(neonBrush()),
        )
    }
}

/** Insignia circular con borde de neón (p. ej. el número de nivel). Decorativa. */
@Composable
fun NeonBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(2.dp, neonBrush(), CircleShape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
    }
}
