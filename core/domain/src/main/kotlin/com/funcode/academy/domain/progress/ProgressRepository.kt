package com.funcode.academy.domain.progress

import com.funcode.academy.domain.lesson.AssessmentScore
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

    /** Registra un intento comprobado (correcto o incorrecto) de un ejercicio. */
    suspend fun recordAttempt(attempt: ExerciseAttempt)

    /** Ejercicios con al menos un intento correcto (se deduce de los intentos guardados). */
    fun observeSolvedExerciseIds(): Flow<Set<String>>

    /**
     * Guarda el resultado de una evaluación de módulo. Solo se conserva el mejor
     * resultado: repetir la evaluación nunca lo empeora ni duplica registros.
     */
    suspend fun recordAssessmentResult(moduleId: String, score: AssessmentScore)

    /** Mejor resultado (0–1) de la evaluación de cada módulo que se haya realizado. */
    fun observeBestAssessmentScores(): Flow<Map<String, Double>>
}
