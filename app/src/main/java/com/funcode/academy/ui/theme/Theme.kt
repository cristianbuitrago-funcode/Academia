package com.funcode.academy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funcode.academy.domain.settings.TextScale
import com.funcode.academy.domain.settings.ThemeMode

// Identidad visual «Academia futurista» (Fase visual): fondo azul marino profundo con
// acentos de neón azul y verde, y violeta como tercer color. Cada par texto/fondo
// cumple el contraste AA (4,5:1); lo comprueba ContrastTest.

/** Colores de neón compartidos por los dos temas (decoración: fondos, bordes, brillos). */
object Neon {
    val Blue = Color(0xFF29B6FF)
    val Green = Color(0xFF39FF8E)
    val Violet = Color(0xFFE36BFF)
    val Navy = Color(0xFF050A18)
}

internal val DarkColors = darkColorScheme(
    primary = Neon.Blue,
    onPrimary = Color(0xFF001A2E),
    primaryContainer = Color(0xFF003A66),
    onPrimaryContainer = Color(0xFFBFE6FF),
    secondary = Neon.Green,
    onSecondary = Color(0xFF002912),
    secondaryContainer = Color(0xFF00522A),
    onSecondaryContainer = Color(0xFFA6FFC9),
    tertiary = Neon.Violet,
    onTertiary = Color(0xFF2E0040),
    tertiaryContainer = Color(0xFF4A1066),
    onTertiaryContainer = Color(0xFFF6D6FF),
    background = Neon.Navy,
    onBackground = Color(0xFFE3ECFF),
    surface = Color(0xFF070E20),
    onSurface = Color(0xFFE3ECFF),
    surfaceVariant = Color(0xFF15244A),
    onSurfaceVariant = Color(0xFFA8BCE6),
    surfaceContainerLowest = Color(0xFF030712),
    surfaceContainerLow = Color(0xFF0A1428),
    surfaceContainer = Color(0xFF0D1A35),
    surfaceContainerHigh = Color(0xFF12213F),
    surfaceContainerHighest = Color(0xFF172A4D),
    outline = Color(0xFF3A5A92),
    outlineVariant = Color(0xFF1F3560),
    error = Color(0xFFFF6B8A),
    onError = Color(0xFF3D0014),
    errorContainer = Color(0xFF6B0A26),
    onErrorContainer = Color(0xFFFFD9DF),
)

internal val LightColors = lightColorScheme(
    primary = Color(0xFF0057B8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E6FF),
    onPrimaryContainer = Color(0xFF001C3A),
    secondary = Color(0xFF006B3C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB4F5CC),
    onSecondaryContainer = Color(0xFF00210F),
    tertiary = Color(0xFF8A2BA8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF6D9FF),
    onTertiaryContainer = Color(0xFF33003F),
    background = Color(0xFFF3F7FF),
    onBackground = Color(0xFF0A1530),
    surface = Color(0xFFF7FAFF),
    onSurface = Color(0xFF0A1530),
    surfaceVariant = Color(0xFFDCE6FA),
    onSurfaceVariant = Color(0xFF33476E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEEF3FF),
    surfaceContainer = Color(0xFFE6EEFF),
    surfaceContainerHigh = Color(0xFFDEE8FD),
    surfaceContainerHighest = Color(0xFFD6E2FA),
    outline = Color(0xFF5C7299),
    outlineVariant = Color(0xFFB7C6E4),
)

/**
 * Títulos con letra monoespaciada (estilo «terminal»), incluida en Android: no se
 * descarga ninguna fuente. El texto normal mantiene la letra por defecto, más legible.
 */
private val FuncodeTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    )
}

private val FuncodeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

/**
 * Tema de la app: oscuro de neón por defecto, con una versión clara disponible en
 * Ajustes. La tipografía usa unidades `sp`, así que respeta el tamaño de letra del sistema.
 */
@Composable
fun FuncodeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FuncodeTypography,
        shapes = FuncodeShapes,
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
