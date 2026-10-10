package com.funcode.academy.domain.progress

/** Tipo de actividad educativa que puede conceder XP. */
enum class XpSourceType {
    EXERCISE,
    LESSON,
    ASSESSMENT,
    CHALLENGE,
}

/**
 * Identifica de forma única una actividad educativa verificada.
 *
 * Una misma fuente solo puede conceder XP una vez: la base de datos lo garantiza
 * con una restricción UNIQUE sobre (type, id).
 */
data class XpSource(val type: XpSourceType, val id: String) {
    init {
        require(id.isNotBlank()) { "El identificador de la actividad no puede estar vacío." }
    }
}

/** Solicitud de XP por haber completado una actividad verificada. */
data class XpAward(val source: XpSource, val amount: Int) {
    init {
        require(amount > 0) { "El XP concedido debe ser positivo (recibido: $amount)." }
    }
}

/** Resultado de intentar registrar XP. */
sealed interface XpAwardResult {
    data class Granted(val amount: Int) : XpAwardResult

    /** La actividad ya había concedido XP; no se suma nada. */
    data object AlreadyGranted : XpAwardResult
}
