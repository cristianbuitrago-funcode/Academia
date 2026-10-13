package com.funcode.academy.domain.rpg

/**
 * Logros iniciales. El [id] es estable (se guarda en la base de datos): no se
 * debe cambiar nunca. Los textos visibles están en los recursos de la app.
 * Los logros no conceden XP, para que no se puedan encadenar recompensas.
 */
enum class Achievement(val id: String) {
    FIRST_LESSON("first_lesson"),
    THREE_LESSONS("three_lessons"),
    FIRST_ASSESSMENT_PASSED("first_assessment_passed"),
    FIRST_MODULE_MASTERED("first_module_mastered"),
    LEVEL_2("level_2"),
    ;

    /** Condición de desbloqueo, comprobada solo con resultados verificados. */
    fun isMet(state: LearningState): Boolean = when (this) {
        FIRST_LESSON -> state.completedLessons >= 1
        THREE_LESSONS -> state.completedLessons >= 3
        FIRST_ASSESSMENT_PASSED -> state.passedAssessments >= 1
        FIRST_MODULE_MASTERED -> state.masteredModules >= 1
        LEVEL_2 -> state.level >= 2
    }

    companion object {
        fun fromId(id: String): Achievement? = entries.firstOrNull { it.id == id }
    }
}

/** Resumen verificado del aprendizaje, a partir del cual se evalúan los logros. */
data class LearningState(
    val completedLessons: Int,
    val passedAssessments: Int,
    /** Módulos con todas sus lecciones completadas y la evaluación aprobada. */
    val masteredModules: Int,
    val level: Int,
)
