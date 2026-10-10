package com.funcode.academy.domain.model

/**
 * Público al que se adapta la presentación del contenido (decisión D3).
 *
 * La adaptación cambia el lenguaje, los ejemplos y la presentación, pero nunca
 * los objetivos de aprendizaje ni los criterios de evaluación.
 */
enum class Audience(val availableInMvp: Boolean) {
    /** Ruta infantil: requiere revisión de privacidad y accesibilidad antes de publicarse. */
    KIDS(availableInMvp = false),
    TEENS(availableInMvp = true),
    ADULTS(availableInMvp = true);

    companion object {
        /** Públicos que el estudiante puede elegir en esta versión. */
        val selectable: List<Audience> = entries.filter { it.availableInMvp }
    }
}
