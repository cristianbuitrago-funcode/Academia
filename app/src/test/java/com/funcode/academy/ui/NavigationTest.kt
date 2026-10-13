package com.funcode.academy.ui

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.funcode.academy.R
import com.funcode.academy.domain.model.Audience
import com.funcode.academy.testing.FakeContentRepository
import com.funcode.academy.testing.FakeProgressRepository
import com.funcode.academy.testing.FakeSettingsRepository
import com.funcode.academy.ui.theme.FuncodeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pruebas de interfaz con Compose ejecutadas en la JVM (Robolectric). No sustituyen la prueba en dispositivo. */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun text(@StringRes id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    private fun clickTab(@StringRes label: Int) {
        rule.onNode(hasText(text(label)) and hasClickAction()).performClick()
    }

    private fun setApp(settings: FakeSettingsRepository = FakeSettingsRepository()) {
        rule.setContent {
            FuncodeTheme {
                FuncodeAppUi(
                    progressRepository = FakeProgressRepository(),
                    settingsRepository = settings,
                    contentRepository = FakeContentRepository(),
                )
            }
        }
    }

    @Test
    fun arrancaEnInicioYMuestraElXpReal() {
        setApp()
        rule.onNodeWithText(text(R.string.home_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.level_title, 1)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.home_total_xp, 0)).assertIsDisplayed()
        rule.onNode(hasText(text(R.string.nav_home)) and hasClickAction()).assertIsSelected()
    }

    @Test
    fun navegaEntreLasCuatroPantallas() {
        setApp()

        clickTab(R.string.nav_map)
        rule.onNodeWithText(text(R.string.map_title)).assertIsDisplayed()

        clickTab(R.string.nav_profile)
        rule.onNodeWithText(text(R.string.profile_title)).assertIsDisplayed()

        clickTab(R.string.nav_settings)
        rule.onNodeWithText(text(R.string.settings_title)).assertIsDisplayed()

        clickTab(R.string.nav_home)
        rule.onNodeWithText(text(R.string.home_title)).assertIsDisplayed()
    }

    @Test
    fun elPerfilSinActividadMuestraElEstadoInicialSinDatosFicticios() {
        setApp()
        clickTab(R.string.nav_profile)
        rule.onNodeWithText(text(R.string.level_title, 1)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.home_total_xp, 0)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.profile_no_rewards)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.profile_achievements_title, 0, 5)).assertExists()
        assertEquals(5, rule.onAllNodes(hasText(text(R.string.achievement_locked))).fetchSemanticsNodes().size)
    }

    @Test
    fun sinContenidoElMapaLoIndicaSinInventarModulos() {
        setApp()
        clickTab(R.string.nav_map)
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodes(hasText(text(R.string.content_unavailable))).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(text(R.string.content_unavailable)).assertIsDisplayed()
    }

    @Test
    fun atrasDesdeUnaPestanaVuelveAInicio() {
        setApp()
        clickTab(R.string.nav_map)
        clickTab(R.string.nav_profile)

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

        rule.onNodeWithText(text(R.string.home_title)).assertIsDisplayed()
    }

    @Test
    fun elegirPublicoLoGuardaEnElRepositorio() {
        val settings = FakeSettingsRepository()
        setApp(settings)
        clickTab(R.string.nav_settings)

        rule.onNode(hasText(text(R.string.audience_adults)) and hasClickAction()).performClick()

        rule.onNode(hasText(text(R.string.audience_adults)) and hasClickAction()).assertIsSelected()
        val saved = runBlocking { settings.audience.first() }
        assertEquals(Audience.ADULTS, saved)
    }

    @Test
    fun laPantallaActualSeConservaAlRecrearLaActividad() {
        val restorationTester = StateRestorationTester(rule)
        restorationTester.setContent {
            FuncodeTheme {
                FuncodeAppUi(
                    progressRepository = FakeProgressRepository(),
                    settingsRepository = FakeSettingsRepository(),
                    contentRepository = FakeContentRepository(),
                )
            }
        }
        clickTab(R.string.nav_map)

        restorationTester.emulateSavedInstanceStateRestore()

        rule.onNodeWithText(text(R.string.map_title)).assertIsDisplayed()
    }
}
