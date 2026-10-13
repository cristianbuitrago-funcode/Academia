package com.funcode.academy.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funcode.academy.domain.lesson.ContentRepository
import com.funcode.academy.domain.lesson.LessonSummary
import com.funcode.academy.domain.progress.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Expone a la pantalla de Inicio el XP total y las lecciones disponibles. */
class HomeViewModel(
    progressRepository: ProgressRepository,
    contentRepository: ContentRepository,
) : ViewModel() {

    /** `null` mientras se lee la base de datos por primera vez. */
    val totalXp: StateFlow<Int?> = progressRepository.observeTotalXp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    private val _lessons = MutableStateFlow<List<LessonSummary>?>(null)

    /** `null` mientras se lee el contenido. */
    val lessons: StateFlow<List<LessonSummary>?> = _lessons.asStateFlow()

    init {
        viewModelScope.launch { _lessons.value = contentRepository.listLessons() }
    }

    companion object {
        fun factory(progressRepository: ProgressRepository, contentRepository: ContentRepository) = viewModelFactory {
            initializer { HomeViewModel(progressRepository, contentRepository) }
        }
    }
}
