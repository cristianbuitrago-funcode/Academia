package com.funcode.academy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta provisional (P10: identidad visual pendiente). Los pares de colores
// texto/fondo de Material 3 están pensados para cumplir el contraste AA.
private val LightColors = lightColorScheme(
    primary = Color(0xFF3F51B5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE0FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF08218A),
    primaryContainer = Color(0xFF293CA0),
    onPrimaryContainer = Color(0xFFDEE0FF),
    secondary = Color(0xFF4FDBC8),
    onSecondary = Color(0xFF003731),
)

/**
 * Tema de la app: claro u oscuro según el sistema. La tipografía usa unidades
 * `sp`, así que respeta el tamaño de letra elegido en los ajustes del sistema.
 */
@Composable
fun FuncodeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
