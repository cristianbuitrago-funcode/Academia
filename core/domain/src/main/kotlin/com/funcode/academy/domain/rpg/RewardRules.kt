package com.funcode.academy.domain.rpg

/**
 * Valores de XP aprobados por el responsable (P6, 2026-10-13, propuesta A).
 *
 * - Ejercicio de lección: 10 + 5 × (dificultad − 1) → 10/15/20/25 XP.
 *   Cada pista revelada antes del primer acierto resta un 25 % del valor, con un
 *   mínimo del 50 %. El resultado se redondea hacia abajo.
 * - Lección completada (todos sus ejercicios resueltos): 20 XP.
 * - Evaluación aprobada (umbral del contenido, 80 %): 100 XP, una sola vez.
 *   Las preguntas de la evaluación no dan XP por separado.
 * - Reto del módulo superado (todos sus pasos, con las lecciones completadas): 50 XP,
 *   una sola vez (Fase 6). Los pasos del reto no dan XP por separado.
 * - Misión completada: 10 XP de bonificación, una sola vez (Fase 6, aprobado).
 * - Los fallos no restan ni suman XP; repetir una actividad recompensada da 0 XP.
 */
object RewardRules {
    const val MIN_DIFFICULTY = 1
    const val MAX_DIFFICULTY = 5
    const val LESSON_XP = 20
    const val ASSESSMENT_PASS_XP = 100
    const val CHALLENGE_XP = 50
    const val MISSION_XP = 10

    private const val HINT_PENALTY_PERCENT = 25
    private const val MIN_PERCENT = 50

    fun exerciseBaseXp(difficulty: Int): Int {
        require(difficulty in MIN_DIFFICULTY..MAX_DIFFICULTY) { "Dificultad no válida: $difficulty" }
        return 10 + 5 * (difficulty - 1)
    }

    fun exerciseXp(difficulty: Int, hintsUsed: Int): Int {
        require(hintsUsed >= 0) { "Pistas no válidas: $hintsUsed" }
        val percent = maxOf(100 - HINT_PENALTY_PERCENT * hintsUsed, MIN_PERCENT)
        return exerciseBaseXp(difficulty) * percent / 100
    }
}
