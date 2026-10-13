package com.funcode.academy.domain.rpg

/**
 * Misiones de objetivos (Fase 6): metas fijas que se calculan con el progreso
 * verificado. No dependen del reloj ni conceden XP, para no premiar la actividad
 * bruta. El [id] es estable.
 */
enum class Mission(val id: String, val target: Int) {
    SOLVE_5_NO_HINTS("solve_5_no_hints", 5),
    SOLVE_20_EXERCISES("solve_20_exercises", 20),
    COMPLETE_5_LESSONS("complete_5_lessons", 5),
    PASS_ASSESSMENT("pass_assessment", 1),
    COMPLETE_CHALLENGE("complete_challenge", 1),
}

data class MissionProgress(val mission: Mission, val current: Int) {
    init {
        require(current in 0..mission.target) { "Progreso no válido: $current de ${mission.target}" }
    }

    val completed: Boolean get() = current == mission.target
    val fraction: Float get() = current.toFloat() / mission.target
}
