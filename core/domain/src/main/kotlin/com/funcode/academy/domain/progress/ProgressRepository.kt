package com.funcode.academy.domain.progress

import kotlinx.coroutines.flow.Flow

/**
 * Acceso al progreso del estudiante. La implementación (Room) vive en :app;
 * el dominio solo conoce este contrato.
 */
interface ProgressRepository {
    /** XP total, calculado a partir del registro de XP (no es un contador editable). */
    fun observeTotalXp(): Flow<Int>

    /** Registra XP para una actividad. Devuelve [XpAwardResult.AlreadyGranted] si ya se había concedido. */
    suspend fun award(award: XpAward): XpAwardResult
}
