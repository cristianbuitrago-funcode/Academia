package com.funcode.academy.ui.lesson

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.funcode.academy.R
import com.funcode.academy.data.content.AssetContentRepository
import com.funcode.academy.domain.lesson.AssessmentScore
import com.funcode.academy.testing.FakeProgressRepository
import com.funcode.academy.testing.FakeSettingsRepository
import com.funcode.academy.ui.FuncodeAppUi
import com.funcode.academy.ui.theme.FuncodeTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recorrido de interfaz con el contenido real de `assets`: Inicio/Mapa → módulo →
 * lección o evaluación → ejercicio → retroalimentación. Robolectric; no sustituye la prueba en dispositivo.
 */
@RunWith(AndroidJUnit4::class)
class LessonFlowTest {

    private companion object {
        const val MODULE_TITLE = "Módulo 1: Introducción a la programación y tu primer programa en Java"
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val progress = FakeProgressRepository()

    private fun text(@StringRes id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    private fun node(matcher: SemanticsMatcher) = rule.onNode(matcher, useUnmergedTree = false)

    /** Espera a que aparezca un texto (el contenido se lee en segundo plano). */
    private fun waitForText(value: String) = rule.waitUntil(timeoutMillis = 10_000) {
        rule.onAllNodes(hasText(value)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun click(label: String) {
        waitForText(label)
        node(hasText(label) and hasClickAction()).performScrollTo().performClick()
    }

    private fun assertShown(value: String) {
        waitForText(value)
        node(hasText(value)).performScrollTo().assertIsDisplayed()
    }

    private fun setApp() = rule.setContent {
        FuncodeTheme {
            FuncodeAppUi(
                progressRepository = progress,
                settingsRepository = FakeSettingsRepository(),
                contentRepository = AssetContentRepository(ApplicationProvider.getApplicationContext<Context>().assets),
            )
        }
    }

    private fun openModuleFromMap() {
        setApp()
        // La barra inferior no está dentro de una zona desplazable: se pulsa sin desplazar.
        waitForText(text(R.string.nav_map))
        node(hasText(text(R.string.nav_map)) and hasClickAction()).performClick()
        assertShown(MODULE_TITLE)
        click(text(R.string.module_open))
        assertShown(text(R.string.module_lessons_title))
    }

    private fun openDemoLessonFirstExercise() {
        openModuleFromMap()
        click(text(R.string.module_lesson_title, 5, "El método main y System.out.println"))
        assertShown(text(R.string.lesson_objectives_title))
        assertShown(text(R.string.draft_label))
        click(text(R.string.lesson_start_exercises))
        assertShown(text(R.string.lesson_exercise_progress, 1, 7))
    }

    @Test
    fun inicioPermiteContinuarPorLaPrimeraLeccionDelModulo() {
        setApp()
        assertShown(text(R.string.home_continue_title))
        assertShown(text(R.string.home_next_lesson, "¿Qué es programar?"))
        assertShown(text(R.string.module_lessons_progress, 0, 7))
        click(text(R.string.home_continue_button))
        assertShown("¿Qué es programar?")
        assertShown(text(R.string.lesson_start_exercises))
    }

    @Test
    fun elModuloMuestraLasLeccionesEnOrdenYLaEvaluacionPendiente() {
        openModuleFromMap()
        assertShown(text(R.string.module_lesson_title, 1, "¿Qué es programar?"))
        assertShown(text(R.string.module_lesson_title, 7, "Leer y comprender programas sencillos"))
        assertShown(text(R.string.module_assessment_not_done))
        assertShown(text(R.string.module_assessment_start))
        // El reto está cerrado: se explica qué falta y no hay botón para abrirlo.
        assertShown("Reto: tu tarjeta de presentación en Java")
        assertShown(text(R.string.challenge_locked, 7))
        assertEquals(0, rule.onAllNodes(hasText(text(R.string.challenge_start))).fetchSemanticsNodes().size)
    }

    @Test
    fun elMejorResultadoGuardadoSeMuestraEnElModulo() {
        runBlocking { progress.recordAssessmentResult("java.m01", AssessmentScore(correct = 7, total = 10)) }
        openModuleFromMap()
        assertShown(text(R.string.module_best_score, 70))
        assertShown(text(R.string.module_assessment_retake))
    }

    @Test
    fun laEvaluacionNoOfreceReintentoTrasUnFallo() {
        openModuleFromMap()
        click(text(R.string.module_assessment_start))
        assertShown(text(R.string.assessment_rules))
        click(text(R.string.lesson_start_assessment))
        assertShown(text(R.string.assessment_progress, 1, 10))
        // q01 es verdadera: se responde "Falso".
        click(text(R.string.answer_false))
        click(text(R.string.lesson_check))
        assertShown(text(R.string.feedback_incorrect_title))
        assertEquals(0, rule.onAllNodes(hasText(text(R.string.lesson_try_again))).fetchSemanticsNodes().size)
        click(text(R.string.assessment_next))
        assertShown(text(R.string.assessment_progress, 2, 10))
        rule.waitForIdle()
        assertEquals(listOf(false), progress.attempts.map { it.correct })
    }

    @Test
    fun respuestaCorrectaMuestraRetroalimentacionYSePuedeAvanzar() {
        openDemoLessonFirstExercise()
        click(text(R.string.answer_true))
        click(text(R.string.lesson_check))
        assertShown(text(R.string.feedback_correct_title))
        assertShown(text(R.string.reward_xp_gained, 10))
        click(text(R.string.lesson_next))
        assertShown(text(R.string.lesson_exercise_progress, 2, 7))
        rule.waitForIdle()
        assertEquals(listOf(true), progress.attempts.map { it.correct })
    }

    @Test
    fun respuestaIncorrectaPermiteReintentarYUsarPistas() {
        openDemoLessonFirstExercise()
        click(text(R.string.lesson_show_hint, 1, 1))
        assertShown("Pista 1: Vuelve a mirar el final de las líneas 3 y 4 del ejemplo.")
        click(text(R.string.answer_false))
        click(text(R.string.lesson_check))
        assertShown(text(R.string.feedback_incorrect_title))
        click(text(R.string.lesson_try_again))
        click(text(R.string.answer_true))
        click(text(R.string.lesson_check))
        assertShown(text(R.string.feedback_correct_title))
        // Una pista: 10 XP − 25 % = 7 XP.
        assertShown(text(R.string.reward_xp_gained, 7))
        rule.waitForIdle()
        assertEquals(listOf(false, true), progress.attempts.map { it.correct })
    }

    @Test
    fun comprobarSinResponderPideCompletarLaRespuesta() {
        openDemoLessonFirstExercise()
        click(text(R.string.lesson_check))
        assertShown(text(R.string.incomplete_nothing_selected))
        assertEquals(emptyList<Boolean>(), progress.attempts.map { it.correct })
    }

    @Test
    fun elPerfilMuestraElXpYLosLogrosConseguidos() {
        openDemoLessonFirstExercise()
        click(text(R.string.answer_true))
        click(text(R.string.lesson_check))
        assertShown(text(R.string.reward_xp_gained, 10))
        waitForText(text(R.string.nav_profile))
        node(hasText(text(R.string.nav_profile)) and hasClickAction()).performClick()
        assertShown(text(R.string.home_total_xp, 10))
        assertShown(text(R.string.profile_rewards_exercises, 1, 10))
        assertShown(text(R.string.profile_achievements_title, 0, 6))
        assertShown(text(R.string.profile_missions_title, 0, 5))
    }
}
