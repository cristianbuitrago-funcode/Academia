package com.funcode.academy.ui.a11y

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.funcode.academy.R
import com.funcode.academy.domain.model.Audience
import com.funcode.academy.domain.settings.LearningPreferences
import com.funcode.academy.domain.settings.TextScale
import com.funcode.academy.domain.settings.ThemeMode
import com.funcode.academy.testing.FakeContentRepository
import com.funcode.academy.testing.FakeProgressRepository
import com.funcode.academy.testing.FakeSettingsRepository
import com.funcode.academy.ui.FuncodeRoot
import com.funcode.academy.ui.components.CodeBlock
import com.funcode.academy.ui.theme.ProvideTextScale
import com.funcode.academy.ui.theme.isDark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Preferencias aplicadas de verdad y etiquetas de accesibilidad (Robolectric + Compose). */
@RunWith(AndroidJUnit4::class)
class PreferencesAndAccessibilityTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun text(@StringRes id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    private fun option(@StringRes id: Int) = rule.onNode(hasText(text(id)) and hasClickAction())

    private fun openSettings(settings: FakeSettingsRepository) {
        rule.setContent { FuncodeRoot(FakeProgressRepository(), settings, FakeContentRepository()) }
        rule.onNode(hasText(text(R.string.nav_settings)) and hasClickAction()).performClick()
    }

    @Test
    fun losTresPublicosSonOpcionesDeUnGrupoDeBotonesDeOpcion() {
        val settings = FakeSettingsRepository()
        openSettings(settings)
        listOf(R.string.audience_kids, R.string.audience_teens, R.string.audience_adults).forEach {
            option(it).performScrollTo().assertIsDisplayed()
            option(it).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        }
        option(R.string.audience_kids).performScrollTo().performClick()
        option(R.string.audience_kids).assertIsSelected()
        assertEquals(Audience.KIDS, settings.current.audience)
    }

    @Test
    fun temaTamanoYPistasSeGuardan() {
        val settings = FakeSettingsRepository()
        openSettings(settings)
        // Oscuro es el predeterminado: se elige Claro para comprobar que el cambio se guarda.
        option(R.string.theme_dark).performScrollTo().assertIsSelected()
        option(R.string.theme_light).performScrollTo().performClick()
        option(R.string.text_scale_extra_large).performScrollTo().performClick()
        val hints = rule.onNode(hasText(text(R.string.settings_hints_label)) and hasClickAction())
        hints.performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On))
            .performClick()
        hints.assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off))
        assertEquals(LearningPreferences(null, ThemeMode.LIGHT, TextScale.EXTRA_LARGE, hintsEnabled = false), settings.current)
        option(R.string.theme_light).assertIsSelected()
    }

    @Test
    fun restablecerPideConfirmacionYNoActuaSiSeCancela() {
        val changed = LearningPreferences(Audience.TEENS, ThemeMode.LIGHT, TextScale.LARGE, hintsEnabled = false)
        val settings = FakeSettingsRepository(changed)
        openSettings(settings)
        rule.onNode(hasText(text(R.string.settings_reset_button)) and hasClickAction()).performScrollTo().performClick()
        rule.onNode(hasText(text(R.string.settings_reset_confirm_text))).assertIsDisplayed()
        rule.onNode(hasText(text(R.string.settings_reset_cancel)) and hasClickAction()).performClick()
        assertEquals(changed, settings.current)

        rule.onNode(hasText(text(R.string.settings_reset_button)) and hasClickAction()).performScrollTo().performClick()
        rule.onNode(hasText(text(R.string.settings_reset_confirm)) and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals(LearningPreferences(), settings.current)
    }

    @Test
    fun elTamanoDeTextoMultiplicaElDelSistema() {
        val base = mutableFloatStateOf(0f)
        val scaled = mutableFloatStateOf(0f)
        rule.setContent {
            base.floatValue = LocalDensity.current.fontScale
            ProvideTextScale(TextScale.EXTRA_LARGE) { scaled.floatValue = LocalDensity.current.fontScale }
        }
        rule.waitForIdle()
        assertEquals(base.floatValue * 1.3f, scaled.floatValue, 0.001f)
    }

    @Test
    fun elTemaRespetaLaPreferencia() {
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
    }

    @Test
    fun cadaLineaDeCodigoSeLeeConSuNumero() {
        rule.setContent { CodeBlock("public class A {\n}") }
        rule.onNode(hasContentDescription(text(R.string.code_line_description, 1, "public class A {"))).assertExists()
        rule.onNode(hasContentDescription(text(R.string.code_line_description, 2, "}"))).assertExists()
    }

    @Test
    fun laBarraDeNivelTieneDescripcionParaTalkBack() {
        rule.setContent { FuncodeRoot(FakeProgressRepository(), FakeSettingsRepository(), FakeContentRepository()) }
        rule.onNode(hasContentDescription(text(R.string.level_progress_description, 2, 0, 100))).assertExists()
    }
}
