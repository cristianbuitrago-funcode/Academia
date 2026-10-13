package com.funcode.academy.domain.lesson.json

import kotlinx.serialization.Serializable

// Formato JSON del contenido (archivos en app/src/main/assets/content/...).
// Son clases planas y tolerantes (campos opcionales): LessonParser decide qué es
// obligatorio para cada tipo y produce mensajes de error claros para los autores.

@Serializable
internal data class LessonJson(
    val schemaVersion: Int? = null,
    val id: String? = null,
    val moduleId: String? = null,
    val languageId: String? = null,
    val locale: String? = null,
    val version: Int? = null,
    val reviewStatus: String? = null,
    /** "lesson" (por defecto) o "assessment". */
    val kind: String? = null,
    val title: String? = null,
    val description: String? = null,
    val objectives: List<ObjectiveJson>? = null,
    val presentations: Map<String, List<SectionJson>>? = null,
    val exercises: List<ExerciseJson>? = null,
    val assessment: AssessmentJson? = null,
)

@Serializable
internal data class ObjectiveJson(val id: String? = null, val text: String? = null)

@Serializable
internal data class SectionJson(
    val type: String? = null,
    val text: String? = null,
    val code: String? = null,
    val lineComments: List<LineCommentJson>? = null,
)

@Serializable
internal data class LineCommentJson(val line: Int? = null, val comment: String? = null)

@Serializable
internal data class AssessmentJson(val exerciseIds: List<String>? = null, val passThreshold: Double? = null)

@Serializable
internal data class OptionJson(val text: String? = null, val feedback: String? = null)

@Serializable
internal data class ExerciseJson(
    val id: String? = null,
    val type: String? = null,
    val objectiveIds: List<String>? = null,
    val difficulty: Int? = null,
    val prompt: String? = null,
    val hints: List<String>? = null,
    val explanation: String? = null,
    val incorrectFeedback: String? = null,
    // multipleChoice
    val options: List<OptionJson>? = null,
    val correctIndices: List<Int>? = null,
    // trueFalse
    val statement: String? = null,
    val correct: Boolean? = null,
    // fillInBlanks
    val template: String? = null,
    val blanks: List<List<String>>? = null,
    // orderLines
    val lines: List<String>? = null,
    val alternativeOrders: List<List<Int>>? = null,
    // predictOutput y findError
    val code: String? = null,
    val acceptedOutputs: List<String>? = null,
    val errorLines: List<Int>? = null,
    // matchPairs
    val left: List<String>? = null,
    val right: List<String>? = null,
    val pairs: List<List<Int>>? = null,
)

@Serializable
internal data class LessonIndexJson(
    val schemaVersion: Int? = null,
    val modules: List<String>? = null,
    val lessons: List<String>? = null,
)

@Serializable
internal data class ModuleJson(
    val schemaVersion: Int? = null,
    val id: String? = null,
    val order: Int? = null,
    val reviewStatus: String? = null,
    val title: String? = null,
    val description: String? = null,
    val lessons: List<String>? = null,
    val assessment: String? = null,
)
