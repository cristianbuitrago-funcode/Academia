package com.funcode.academy.ui.a11y

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
import com.funcode.academy.domain.settings.LearningPreferences
import com.funcode.academy.domain.settings.TextScale
import com.funcode.academy.testing.FakeProgressRepository
import com.funcode.academy.testing.FakeSettingsRepository
import com.funcode.academy.ui.FuncodeRoot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Con el texto del sistema al 200 % y el de la app en «Muy grande» (260 % en total),
 * todo sigue siendo alcanzable desplazándose: Inicio → lección → ejercicio → retroalimentación.
 */
@RunWith(AndroidJUnit4::class)
@Config(fontScale = 2.0f)
class LargeTextTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun text(@StringRes id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    private fun waitFor(value: String) = rule.waitUntil(10_000) { rule.onAllNodes(hasText(value)).fetchSemanticsNodes().isNotEmpty() }

    private fun node(matcher: SemanticsMatcher) = rule.onNode(matcher)

    private fun click(label: String) {
        waitFor(label)
        node(hasText(label) and hasClickAction()).performScrollTo().performClick()
    }

    @Test
    fun laLeccionSigueSiendoUsableConTextoMuyGrande() {
        rule.setContent {
            FuncodeRoot(
                FakeProgressRepository(),
                FakeSettingsRepository(LearningPreferences(textScale = TextScale.EXTRA_LARGE)),
                AssetContentRepository(ApplicationProvider.getApplicationContext<Context>().assets),
            )
        }
        click(text(R.string.home_continue_button))
        click(text(R.string.lesson_start_exercises))
        click(text(R.string.answer_false)) // l01.e01: la respuesta correcta es «Falso»
        click(text(R.string.lesson_check))
        waitFor(text(R.string.feedback_correct_title))
        node(hasText(text(R.string.feedback_correct_title))).performScrollTo().assertIsDisplayed()
        node(hasText(text(R.string.lesson_next)) and hasClickAction()).performScrollTo().assertIsDisplayed()
    }
}
