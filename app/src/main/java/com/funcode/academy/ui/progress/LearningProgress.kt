package com.funcode.academy.ui.progress

import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.progress.ModuleProgress
import com.funcode.academy.domain.progress.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

/**
 * Progreso de todos los módulos, recalculado cada vez que cambian los intentos o los
 * resultados de evaluación guardados en Room. Nada de esto se guarda aparte.
 * Si algo falla al leer, se emite una lista vacía (la UI muestra «contenido no
 * disponible») en lugar de cerrar la app.
 */
fun observeModulesProgress(content: ContentRepository, progress: ProgressRepository): Flow<List<ModuleProgress>> =
    combine(
        flow { emit(content.listModules()) },
        progress.observeSolvedExerciseIds(),
        progress.observeBestAssessmentScores(),
    ) { modules, solved, scores -> modules.map { ModuleProgress.of(it, solved, scores) } }
        .catch { emit(emptyList()) }
