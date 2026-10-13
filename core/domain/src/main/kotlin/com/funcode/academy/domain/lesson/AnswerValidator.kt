package com.funcode.academy.domain.lesson

/**
 * Comprueba respuestas según las reglas del contenido. Es una función pura: no
 * ejecuta código ni accede a la base de datos.
 */
object AnswerValidator {

    fun validate(content: ExerciseContent, answer: Answer): AnswerResult = when (content) {
        is ExerciseContent.MultipleChoice -> multipleChoice(content, answer as? Answer.Choice ?: wrongType(content, answer))
        is ExerciseContent.TrueFalse -> trueFalse(content, answer as? Answer.TrueFalse ?: wrongType(content, answer))
        is ExerciseContent.FillInBlanks -> fillInBlanks(content, answer as? Answer.Blanks ?: wrongType(content, answer))
        is ExerciseContent.OrderLines -> orderLines(content, answer as? Answer.Order ?: wrongType(content, answer))
        is ExerciseContent.PredictOutput -> predictOutput(content, answer as? Answer.Output ?: wrongType(content, answer))
        is ExerciseContent.FindError -> findError(content, answer as? Answer.ErrorLines ?: wrongType(content, answer))
        is ExerciseContent.MatchPairs -> matchPairs(content, answer as? Answer.Pairs ?: wrongType(content, answer))
    }

    private fun wrongType(content: ExerciseContent, answer: Answer): Nothing =
        throw IllegalArgumentException("Respuesta ${answer::class.simpleName} no válida para ${content::class.simpleName}")

    private fun multipleChoice(content: ExerciseContent.MultipleChoice, answer: Answer.Choice): AnswerResult {
        if (answer.selected.isEmpty()) return AnswerResult.Incomplete(IncompleteReason.NOTHING_SELECTED)
        if (answer.selected == content.correctIndices) return AnswerResult.Correct
        // Con una sola opción elegida se puede dar su retroalimentación específica.
        val feedback = answer.selected.singleOrNull()?.let { content.options.getOrNull(it)?.feedback }
        return AnswerResult.Incorrect(optionFeedback = feedback)
    }

    private fun trueFalse(content: ExerciseContent.TrueFalse, answer: Answer.TrueFalse): AnswerResult = when (answer.value) {
        null -> AnswerResult.Incomplete(IncompleteReason.NOTHING_SELECTED)
        content.correct -> AnswerResult.Correct
        else -> AnswerResult.Incorrect()
    }

    private fun fillInBlanks(content: ExerciseContent.FillInBlanks, answer: Answer.Blanks): AnswerResult {
        val total = content.acceptedAnswers.size
        val values = answer.values
        if (values.size != total || values.any { it.isBlank() }) {
            return AnswerResult.Incomplete(IncompleteReason.EMPTY_BLANKS)
        }
        val correct = content.acceptedAnswers.indices.count { i ->
            val given = CodeText.tokenize(values[i])
            content.acceptedAnswers[i].any { CodeText.tokenize(it) == given }
        }
        return if (correct == total) AnswerResult.Correct else AnswerResult.Incorrect(correctParts = correct, totalParts = total)
    }

    private fun orderLines(content: ExerciseContent.OrderLines, answer: Answer.Order): AnswerResult {
        val indices = answer.lineIndices
        if (indices.sorted() != content.lines.indices.toList()) {
            return AnswerResult.Incomplete(IncompleteReason.INVALID_ORDER)
        }
        // Se compara el texto resultante, no los índices: dos líneas iguales (p. ej. dos
        // llaves de cierre) son intercambiables sin cambiar el programa.
        fun textOf(order: List<Int>) = order.map { CodeText.normalizeLine(content.lines[it]) }
        val given = textOf(indices)
        val valid = listOf(content.lines.indices.toList()) + content.alternativeOrders
        return if (valid.any { textOf(it) == given }) AnswerResult.Correct else AnswerResult.Incorrect()
    }

    private fun predictOutput(content: ExerciseContent.PredictOutput, answer: Answer.Output): AnswerResult {
        val given = CodeText.normalizeOutput(answer.text)
        if (given.isEmpty()) return AnswerResult.Incomplete(IncompleteReason.EMPTY_OUTPUT)
        return if (content.acceptedOutputs.any { CodeText.normalizeOutput(it) == given }) {
            AnswerResult.Correct
        } else {
            AnswerResult.Incorrect()
        }
    }

    private fun findError(content: ExerciseContent.FindError, answer: Answer.ErrorLines): AnswerResult = when {
        answer.lines.isEmpty() -> AnswerResult.Incomplete(IncompleteReason.NOTHING_SELECTED)
        answer.lines == content.errorLines -> AnswerResult.Correct
        else -> AnswerResult.Incorrect(
            correctParts = answer.lines.count { it in content.errorLines },
            totalParts = content.errorLines.size,
        )
    }

    private fun matchPairs(content: ExerciseContent.MatchPairs, answer: Answer.Pairs): AnswerResult {
        val pairs = answer.pairs
        if (pairs.keys != content.left.indices.toSet()) return AnswerResult.Incomplete(IncompleteReason.NOT_ALL_PAIRS)
        if (pairs.values.toSet().size != pairs.size) return AnswerResult.Incomplete(IncompleteReason.DUPLICATED_PAIR)
        val correct = pairs.count { (l, r) -> content.correctPairs[l] == r }
        return if (correct == content.left.size) {
            AnswerResult.Correct
        } else {
            AnswerResult.Incorrect(correctParts = correct, totalParts = content.left.size)
        }
    }
}
