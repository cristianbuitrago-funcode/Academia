package com.funcode.academy.domain.progress

/**
 * Intento comprobado de un ejercicio. No se guarda el texto de la respuesta (privacidad).
 * Que un ejercicio esté resuelto se deduce de sus intentos, sin tablas adicionales.
 */
data class ExerciseAttempt(
    val exerciseId: String,
    val correct: Boolean,
    val hintsUsed: Int,
) {
    init {
        require(exerciseId.isNotBlank()) { "El identificador del ejercicio no puede estar vacío." }
        require(hintsUsed >= 0) { "hintsUsed no puede ser negativo." }
    }
}
