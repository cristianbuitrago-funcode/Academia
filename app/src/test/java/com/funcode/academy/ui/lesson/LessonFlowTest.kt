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
import com.funcode.academy.testing.FakeProgressRepository
import com.funcode.academy.testing.FakeSettingsRepository
import com.funcode.academy.ui.FuncodeAppUi
import com.funcode.academy.ui.theme.FuncodeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recorrido de interfaz con el contenido real de `assets`: Inicio → lección →
 * ejercicio → retroalimentación. Robolectric; no sustituye la prueba en dispositivo.
 */
@RunWith(AndroidJUnit4::class)
class LessonFlowTest {

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

    private fun openDemoLessonFirstExercise() {
        rule.setContent {
            FuncodeTheme {
                FuncodeAppUi(
                    progressRepository = progress,
                    settingsRepository = FakeSettingsRepository(),
                    contentRepository = AssetContentRepository(ApplicationProvider.getApplicationContext<Context>().assets),
                )
            }
        }
        assertShown("Tu primer programa: mostrar texto en pantalla")
        click(text(R.string.home_open_lesson))
        assertShown(text(R.string.lesson_objectives_title))
        assertShown(text(R.string.draft_label))
        click(text(R.string.lesson_start_exercises))
        assertShown(text(R.string.lesson_exercise_progress, 1, 7))
    }

    @Test
    fun respuestaCorrectaMuestraRetroalimentacionYSePuedeAvanzar() {
        openDemoLessonFirstExercise()
        click(text(R.string.answer_true))
        click(text(R.string.lesson_check))
        assertShown(text(R.string.feedback_correct_title))
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
}
