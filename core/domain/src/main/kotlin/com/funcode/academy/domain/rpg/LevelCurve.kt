package com.funcode.academy.domain.rpg

/**
 * Curva de niveles aprobada (P6, 2026-10-13): pasar del nivel n al n+1 cuesta 100 × n XP.
 * XP acumulado para empezar el nivel n: 50 × n × (n − 1) → 0, 100, 300, 600, 1000…
 *
 * El nivel se calcula siempre a partir del XP total, así que es determinista y la
 * curva puede cambiarse más adelante sin perder datos.
 */
object LevelCurve {
    /** XP total necesario para empezar el [level] (el nivel 1 empieza en 0). */
    fun xpToReach(level: Int): Int {
        require(level >= 1) { "Nivel no válido: $level" }
        return 50 * level * (level - 1)
    }

    fun progressFor(totalXp: Int): LevelProgress {
        require(totalXp >= 0) { "El XP total no puede ser negativo: $totalXp" }
        var level = 1
        while (xpToReach(level + 1) <= totalXp) level++
        val start = xpToReach(level)
        return LevelProgress(
            level = level,
            totalXp = totalXp,
            xpIntoLevel = totalXp - start,
            xpForNextLevel = xpToReach(level + 1) - start,
        )
    }
}

/** Nivel actual y avance hacia el siguiente. */
data class LevelProgress(
    val level: Int,
    val totalXp: Int,
    /** XP conseguido dentro del nivel actual. */
    val xpIntoLevel: Int,
    /** XP que cuesta el nivel actual completo. */
    val xpForNextLevel: Int,
) {
    val xpRemaining: Int get() = xpForNextLevel - xpIntoLevel
    val fraction: Float get() = xpIntoLevel.toFloat() / xpForNextLevel
}
