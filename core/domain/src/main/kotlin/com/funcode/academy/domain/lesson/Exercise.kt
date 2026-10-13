package com.funcode.academy.domain.lesson

/** Datos comunes a todos los ejercicios. El contenido específico de cada tipo está en [content]. */
data class Exercise(
    /** Identificador estable, p. ej. `java.m02.l01.e03`. */
    val id: String,
    val objectiveIds: List<String>,
    /** 1 (muy fácil) a 5 (difícil). */
    val difficulty: Int,
    /** Instrucciones que ve el estudiante. */
    val prompt: String,
    val content: ExerciseContent,
    /** Pistas progresivas: cada una ayuda un poco más, sin dar la solución. */
    val hints: List<String>,
    /** Explicación de la respuesta correcta; se muestra al acertar. */
    val explanation: String,
    /** Orientación general cuando la respuesta es incorrecta. */
    val incorrectFeedback: String,
)

/** Los siete tipos de ejercicio de la Fase 3. */
sealed interface ExerciseContent {

    /** Selección múltiple. Si hay más de una opción correcta, el estudiante debe marcarlas todas. */
    data class MultipleChoice(val options: List<ChoiceOption>, val correctIndices: Set<Int>) : ExerciseContent {
        val allowsMultipleSelection: Boolean get() = correctIndices.size > 1
    }

    /** Verdadero o falso sobre una afirmación (la afirmación está en `Exercise.prompt`). */
    data class TrueFalse(val statement: String, val correct: Boolean) : ExerciseContent

    /**
     * Completar huecos en código. [template] contiene marcadores `{{0}}`, `{{1}}`…;
     * [acceptedAnswers] tiene, para cada hueco, las respuestas válidas.
     */
    data class FillInBlanks(val template: String, val acceptedAnswers: List<List<String>>) : ExerciseContent

    /**
     * Ordenar líneas. [lines] está en el orden correcto; la interfaz las muestra desordenadas.
     * [alternativeOrders] declara otros órdenes también válidos (índices de [lines]).
     */
    data class OrderLines(val lines: List<String>, val alternativeOrders: List<List<Int>>) : ExerciseContent

    /** Predecir la salida de un programa (no se ejecuta; se compara con la salida esperada). */
    data class PredictOutput(val code: String, val acceptedOutputs: List<String>) : ExerciseContent

    /** Encontrar las líneas con error. [errorLines] se numeran desde 1. */
    data class FindError(val code: String, val errorLines: Set<Int>) : ExerciseContent {
        val lines: List<String> get() = code.lines()
    }

    /** Relacionar conceptos: [correctPairs] asocia cada índice de [left] con uno de [right]. */
    data class MatchPairs(val left: List<String>, val right: List<String>, val correctPairs: Map<Int, Int>) :
        ExerciseContent
}

data class ChoiceOption(val text: String, val feedback: String?)
