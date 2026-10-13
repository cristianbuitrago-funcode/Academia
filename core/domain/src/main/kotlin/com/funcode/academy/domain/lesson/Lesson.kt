package com.funcode.academy.domain.lesson

import com.funcode.academy.domain.model.Audience

/**
 * Una lección ya validada. Se construye solo a través de [LessonParser], que
 * garantiza que los identificadores, referencias y respuestas son coherentes.
 */
data class Lesson(
    /** Identificador estable, p. ej. `java.m02.l01`. No debe cambiar entre versiones. */
    val id: String,
    val moduleId: String,
    val languageId: String,
    val locale: String,
    /** Versión del contenido; aumenta cada vez que el texto o los ejercicios cambian. */
    val version: Int,
    val reviewStatus: ReviewStatus,
    /** Lección normal o evaluación de módulo. */
    val kind: LessonKind,
    val title: String,
    val description: String,
    /** Núcleo común a todos los públicos (decisión D3). */
    val objectives: List<LearningObjective>,
    /** Teoría y ejemplos por variante de presentación; siempre existe [PresentationVariant.DEFAULT]. */
    val presentations: Map<PresentationVariant, List<Section>>,
    val exercises: List<Exercise>,
    val assessment: Assessment?,
    /** Datos del proyecto guiado; obligatorio solo en los retos ([LessonKind.CHALLENGE]). */
    val project: ProjectInfo? = null,
) {
    /** Secciones para un público: usa su variante si existe y, si no, la variante por defecto. */
    fun sectionsFor(audience: Audience?): List<Section> {
        val variant = when (audience) {
            Audience.KIDS -> PresentationVariant.KIDS
            Audience.TEENS -> PresentationVariant.TEENS
            Audience.ADULTS -> PresentationVariant.ADULTS
            null -> PresentationVariant.DEFAULT
        }
        return presentations[variant] ?: presentations.getValue(PresentationVariant.DEFAULT)
    }
}

/**
 * Proyecto guiado: situación que plantea el problema, conceptos que hay que conocer
 * antes de empezar y resumen final de lo aprendido.
 */
data class ProjectInfo(val situation: String, val priorConcepts: List<String>, val recap: List<String>)

/**
 * [LESSON]: teoría y práctica, con pistas y reintentos.
 * [ASSESSMENT]: evaluación del módulo; una sola respuesta por pregunta y sin pistas.
 */
enum class LessonKind {
    LESSON,
    ASSESSMENT,

    /** Reto guiado del módulo: un mini-proyecto en varios pasos, con pistas y reintentos. */
    CHALLENGE,
}

/** Estado de revisión del contenido (decisión D7). Solo el responsable lo cambia a [APPROVED]. */
enum class ReviewStatus { DRAFT, APPROVED }

enum class PresentationVariant { DEFAULT, KIDS, TEENS, ADULTS }

data class LearningObjective(val id: String, val text: String)

/** Bloque de la explicación teórica. */
sealed interface Section {
    data class Text(val text: String) : Section

    /** Nota destacada (consejo, advertencia). */
    data class Callout(val text: String) : Section

    /**
     * Ejemplo de código Java. [lineComments] explica líneas concretas (numeradas desde 1)
     * sin ensuciar el código mostrado.
     */
    data class Code(val code: String, val lineComments: List<LineComment>) : Section {
        val lines: List<String> get() = code.lines()
    }
}

data class LineComment(val line: Int, val comment: String)

/**
 * Evaluación: qué ejercicios cuentan para el resultado y, si está decidido, qué
 * fracción hay que acertar al primer intento para aprobar. [passThreshold] es
 * `null` mientras el umbral no esté aprobado (P6): entonces solo se informa del
 * resultado, sin declarar aprobado o suspenso.
 */
data class Assessment(val exerciseIds: List<String>, val passThreshold: Double?)

/** Datos mínimos para listar lecciones sin cargarlas completas en la interfaz. */
data class LessonSummary(
    val id: String,
    val moduleId: String,
    val title: String,
    val description: String,
    val reviewStatus: ReviewStatus,
    val kind: LessonKind,
    val exerciseIds: List<String>,
) {
    val exerciseCount: Int get() = exerciseIds.size
}

fun Lesson.toSummary() = LessonSummary(id, moduleId, title, description, reviewStatus, kind, exercises.map { it.id })
