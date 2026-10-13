package com.funcode.academy.domain.mission

import com.funcode.academy.domain.lesson.ReviewStatus

/** Tipos de misión (Fase 6). El nombre JSON es estable. */
enum class MissionType(val jsonName: String) {
    /** Estudiar lecciones y comprender sus conceptos. */
    LEARNING("learning"),

    /** Resolver un conjunto definido de ejercicios. */
    PRACTICE("practice"),

    /** Superar la evaluación del módulo. */
    ASSESSMENT("assessment"),

    /** Proyecto guiado: el reto del módulo. */
    PROJECT("project"),
}

/**
 * Tarea verificable de una misión. Siempre apunta a contenido existente (lecciones,
 * ejercicios, evaluaciones o retos): las misiones no tienen actividades propias.
 */
sealed interface MissionTask {
    /** Resolver todos los ejercicios de una lección. */
    data class CompleteLesson(val lessonId: String) : MissionTask

    /** Resolver un conjunto concreto de ejercicios de lecciones o del reto. */
    data class SolveExercises(val exerciseIds: List<String>, val description: String) : MissionTask

    /** Alcanzar el umbral de aprobación aprobado (80 %) en una evaluación. */
    data class PassAssessment(val assessmentId: String) : MissionTask

    /** Resolver todos los pasos de un reto (proyecto guiado). */
    data class CompleteChallenge(val challengeId: String) : MissionTask
}

/** Misión tal como está en el contenido (`missions/<id>.json`). */
data class MissionDefinition(
    /** Identificador estable: también identifica su recompensa en el registro de XP. */
    val id: String,
    val moduleId: String,
    val order: Int,
    val version: Int,
    val reviewStatus: ReviewStatus,
    val type: MissionType,
    val title: String,
    val description: String,
    /** Objetivo educativo: qué aprende o practica el estudiante. */
    val objective: String,
    /** Misiones que hay que completar antes. */
    val prerequisiteIds: List<String>,
    val tasks: List<MissionTask>,
)

sealed interface MissionParseResult {
    data class Valid(val mission: MissionDefinition) : MissionParseResult
    data class Invalid(val errors: List<String>) : MissionParseResult
}

enum class MissionStatus { LOCKED, AVAILABLE, IN_PROGRESS, COMPLETED }

/** Avance de una tarea. [activityId] es la lección, evaluación o reto que hay que abrir para avanzar. */
data class TaskProgress(val task: MissionTask, val current: Int, val total: Int, val activityId: String) {
    val done: Boolean get() = current == total
}

/** Estado de una misión, siempre calculado a partir de los resultados verificados guardados. */
data class MissionProgress(
    val mission: MissionDefinition,
    val status: MissionStatus,
    val tasks: List<TaskProgress>,
    /** Requisitos sin completar (vacío salvo si está bloqueada). */
    val missingPrerequisites: List<MissionDefinition>,
) {
    val completedTasks: Int get() = tasks.count { it.done }
}
