package com.funcode.academy.domain.lesson

/**
 * Módulo de la ruta: lecciones en el orden recomendado y, opcionalmente, una
 * evaluación final y un reto guiado. Se define en `modules/<id>.json` y se valida contra las
 * lecciones existentes con [ModuleAssembler].
 */
data class CourseModule(
    val id: String,
    val order: Int,
    val title: String,
    val description: String,
    val reviewStatus: ReviewStatus,
    val lessons: List<LessonSummary>,
    val assessment: LessonSummary?,
    /** Reto guiado; se abre al completar todas las lecciones. */
    val challenge: LessonSummary? = null,
)

/** Definición tal como está en el archivo, antes de comprobar las referencias. */
data class ModuleDefinition(
    val id: String,
    val order: Int,
    val title: String,
    val description: String,
    val reviewStatus: ReviewStatus,
    val lessonIds: List<String>,
    val assessmentId: String?,
    val challengeId: String? = null,
)

sealed interface ModuleLoadResult {
    data class Loaded(val module: CourseModule) : ModuleLoadResult
    data object NotFound : ModuleLoadResult
    data class Invalid(val errors: List<String>) : ModuleLoadResult
}

/** Comprueba que un módulo solo hace referencia a lecciones válidas y coherentes. */
object ModuleAssembler {

    fun assemble(definition: ModuleDefinition, lessons: Map<String, LessonSummary>): ModuleLoadResult {
        val errors = mutableListOf<String>()
        val id = definition.id
        if (definition.lessonIds.isEmpty()) errors += "El módulo '$id' no tiene lecciones"
        definition.lessonIds.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            .forEach { errors += "El módulo '$id' repite la lección '$it'" }

        fun resolve(lessonId: String, expected: LessonKind): LessonSummary? {
            val lesson = lessons[lessonId]
            when {
                lesson == null -> errors += "El módulo '$id' hace referencia a una lección inexistente o inválida: '$lessonId'"
                lesson.moduleId != id -> errors += "La lección '$lessonId' pertenece a '${lesson.moduleId}', no a '$id'"
                lesson.kind != expected -> errors += "La lección '$lessonId' debe ser de tipo $expected"
            }
            return lesson
        }

        val ordered = definition.lessonIds.mapNotNull { resolve(it, LessonKind.LESSON) }
        val assessment = definition.assessmentId?.let { resolve(it, LessonKind.ASSESSMENT) }
        val challenge = definition.challengeId?.let { resolve(it, LessonKind.CHALLENGE) }
        if (errors.isNotEmpty()) return ModuleLoadResult.Invalid(errors)
        return ModuleLoadResult.Loaded(
            CourseModule(
                id, definition.order, definition.title, definition.description, definition.reviewStatus,
                ordered, assessment, challenge,
            ),
        )
    }
}
