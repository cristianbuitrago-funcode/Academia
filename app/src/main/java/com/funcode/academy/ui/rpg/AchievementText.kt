package com.funcode.academy.ui.rpg

import androidx.annotation.StringRes
import com.funcode.academy.R
import com.funcode.academy.domain.rpg.Achievement
import com.funcode.academy.domain.rpg.Mission

/** Textos visibles de cada logro y misión (los identificadores viven en el dominio). */
@get:StringRes
val Achievement.titleRes: Int
    get() = when (this) {
        Achievement.FIRST_LESSON -> R.string.achievement_first_lesson_title
        Achievement.THREE_LESSONS -> R.string.achievement_three_lessons_title
        Achievement.FIRST_ASSESSMENT_PASSED -> R.string.achievement_first_assessment_title
        Achievement.FIRST_MODULE_MASTERED -> R.string.achievement_first_module_title
        Achievement.LEVEL_2 -> R.string.achievement_level_2_title
        Achievement.FIRST_CHALLENGE -> R.string.achievement_first_challenge_title
    }

@get:StringRes
val Achievement.descriptionRes: Int
    get() = when (this) {
        Achievement.FIRST_LESSON -> R.string.achievement_first_lesson_description
        Achievement.THREE_LESSONS -> R.string.achievement_three_lessons_description
        Achievement.FIRST_ASSESSMENT_PASSED -> R.string.achievement_first_assessment_description
        Achievement.FIRST_MODULE_MASTERED -> R.string.achievement_first_module_description
        Achievement.LEVEL_2 -> R.string.achievement_level_2_description
        Achievement.FIRST_CHALLENGE -> R.string.achievement_first_challenge_description
    }

/** Texto de cada misión (los identificadores y las metas viven en el dominio). */
@get:StringRes
val Mission.titleRes: Int
    get() = when (this) {
        Mission.SOLVE_5_NO_HINTS -> R.string.mission_solve_5_no_hints
        Mission.SOLVE_20_EXERCISES -> R.string.mission_solve_20_exercises
        Mission.COMPLETE_5_LESSONS -> R.string.mission_complete_5_lessons
        Mission.PASS_ASSESSMENT -> R.string.mission_pass_assessment
        Mission.COMPLETE_CHALLENGE -> R.string.mission_complete_challenge
    }
