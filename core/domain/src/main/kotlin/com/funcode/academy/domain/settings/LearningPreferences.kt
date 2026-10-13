package com.funcode.academy.domain.settings

import com.funcode.academy.domain.model.Audience

/** Tema de la app. [SYSTEM] sigue el modo claro u oscuro del sistema. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Tamaño del texto de la app. Se multiplica por el tamaño elegido en los ajustes del
 * sistema, que se sigue respetando.
 */
enum class TextScale(val factor: Float) {
    NORMAL(1.0f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f),
}

/** Preferencias del estudiante. Se guardan solo en el dispositivo y se pueden cambiar en cualquier momento. */
data class LearningPreferences(
    /** Variante de presentación elegida voluntariamente; `null` = presentación común. */
    val audience: Audience? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val textScale: TextScale = TextScale.NORMAL,
    /** Mostrar el botón de pistas en lecciones y proyectos (no cambia el XP ni la corrección). */
    val hintsEnabled: Boolean = true,
)
