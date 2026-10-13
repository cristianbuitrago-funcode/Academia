package com.funcode.academy.domain.lesson

/** Respuesta del estudiante. Cada tipo de ejercicio acepta un tipo de respuesta. */
sealed interface Answer {
    data class Choice(val selected: Set<Int>) : Answer
    data class TrueFalse(val value: Boolean?) : Answer
    data class Blanks(val values: List<String>) : Answer

    /** Índices de las líneas originales en el orden elegido por el estudiante. */
    data class Order(val lineIndices: List<Int>) : Answer
    data class Output(val text: String) : Answer

    /** Líneas marcadas como erróneas (desde 1). */
    data class ErrorLines(val lines: Set<Int>) : Answer

    /** Índice de `left` → índice de `right`. */
    data class Pairs(val pairs: Map<Int, Int>) : Answer
}

/** Resultado de comprobar una respuesta. No implica que se haya ejecutado ningún código. */
sealed interface AnswerResult {
    data object Correct : AnswerResult

    /**
     * La respuesta está completa pero no es correcta. [optionFeedback] contiene la
     * retroalimentación específica de la opción elegida, si el contenido la define.
     * [correctParts]/[totalParts] indican cuántas partes acertó (huecos, parejas).
     */
    data class Incorrect(
        val optionFeedback: String? = null,
        val correctParts: Int? = null,
        val totalParts: Int? = null,
    ) : AnswerResult

    /** No se puede comprobar todavía: falta completar algo. No cuenta como intento. */
    data class Incomplete(val reason: IncompleteReason) : AnswerResult
}

enum class IncompleteReason {
    NOTHING_SELECTED,
    EMPTY_BLANKS,
    EMPTY_OUTPUT,
    NOT_ALL_PAIRS,
    DUPLICATED_PAIR,
    INVALID_ORDER,
}
