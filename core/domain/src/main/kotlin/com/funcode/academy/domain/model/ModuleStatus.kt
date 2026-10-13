package com.funcode.academy.domain.model

/** Estado de un módulo de la ruta de aprendizaje en el progreso del estudiante. */
enum class ModuleStatus {
    LOCKED,
    AVAILABLE,
    COMPLETED,

    /** Evaluación del módulo aprobada. */
    MASTERED,
}
