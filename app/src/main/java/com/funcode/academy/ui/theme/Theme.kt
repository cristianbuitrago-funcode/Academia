package com.funcode.academy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.funcode.academy.domain.settings.TextScale
import com.funcode.academy.domain.settings.ThemeMode

// Paleta provisional (P10: identidad visual pendiente). Los pares de colores
// texto/fondo de Material 3 están pensados para cumplir el contraste AA.
internal val LightColors = lightColorScheme(
    primary = Color(0xFF3F51B5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDEE0FF),
    onPrimaryContainer = Color(0xFF00105C),
    // 0xFF00897B daba 4,3:1 con texto blanco (< 4,5:1, AA); se oscurece en la Fase 7.
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF08218A),
    primaryContainer = Color(0xFF293CA0),
    onPrimaryContainer = Color(0xFFDEE0FF),
    secondary = Color(0xFF4FDBC8),
    onSecondary = Color(0xFF003731),
)

/**
 * Tema de la app: claro u oscuro (por defecto, según el sistema). La tipografía usa
 * unidades `sp`, así que respeta el tamaño de letra elegido en los ajustes del sistema.
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

/** Decide el modo oscuro según la preferencia del estudiante. */
fun ThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * Multiplica el tamaño de letra del sistema por el de la app. Todo el texto usa `sp`,
 * así que se aplica a todas las pantallas sin tocar cada una.
 */
@Composable
fun ProvideTextScale(scale: TextScale, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * scale.factor),
        content = content,
    )
}
