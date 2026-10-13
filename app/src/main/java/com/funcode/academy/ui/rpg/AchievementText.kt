package com.funcode.academy.ui.rpg

import androidx.annotation.StringRes
import com.funcode.academy.R
import com.funcode.academy.domain.rpg.Achievement

/** Textos visibles de cada logro (los identificadores viven en el dominio). */
@get:StringRes
val Achievement.titleRes: Int
    get() = when (this) {
        Achievement.FIRST_LESSON -> R.string.achievement_first_lesson_title
        Achievement.THREE_LESSONS -> R.string.achievement_three_lessons_title
        Achievement.FIRST_ASSESSMENT_PASSED -> R.string.achievement_first_assessment_title
        Achievement.FIRST_MODULE_MASTERED -> R.string.achievement_first_module_title
        Achievement.LEVEL_2 -> R.string.achievement_level_2_title
    }

@get:StringRes
val Achievement.descriptionRes: Int
    get() = when (this) {
        Achievement.FIRST_LESSON -> R.string.achievement_first_lesson_description
        Achievement.THREE_LESSONS -> R.string.achievement_three_lessons_description
        Achievement.FIRST_ASSESSMENT_PASSED -> R.string.achievement_first_assessment_description
        Achievement.FIRST_MODULE_MASTERED -> R.string.achievement_first_module_description
        Achievement.LEVEL_2 -> R.string.achievement_level_2_description
    }
