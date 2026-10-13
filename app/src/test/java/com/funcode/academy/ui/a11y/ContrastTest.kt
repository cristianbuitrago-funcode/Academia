package com.funcode.academy.ui.a11y

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.funcode.academy.ui.theme.DarkColors
import com.funcode.academy.ui.theme.LightColors
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Contraste WCAG 2.x de los pares texto/fondo del tema (mínimo 4,5:1, nivel AA para
 * texto normal). Solo comprueba la paleta; no sustituye una auditoría completa.
 */
class ContrastTest {

    private fun channel(c: Float) = if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color) = 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun pairs(s: ColorScheme) = mapOf(
        "primary" to (s.onPrimary to s.primary),
        "primaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
        "secondary" to (s.onSecondary to s.secondary),
        "secondaryContainer" to (s.onSecondaryContainer to s.secondaryContainer),
        "tertiaryContainer" to (s.onTertiaryContainer to s.tertiaryContainer),
        "surface" to (s.onSurface to s.surface),
        "surfaceVariant" to (s.onSurfaceVariant to s.surfaceVariant),
        "background" to (s.onBackground to s.background),
        "error" to (s.onError to s.error),
    )

    @Test
    fun losParesDeTextoYFondoCumplenAA() {
        listOf("claro" to LightColors, "oscuro" to DarkColors).forEach { (name, scheme) ->
            pairs(scheme).forEach { (pair, colors) ->
                val ratio = contrast(colors.first, colors.second)
                assertTrue("$name/$pair: ${"%.2f".format(ratio)}:1", ratio >= 4.5)
            }
        }
    }
}
