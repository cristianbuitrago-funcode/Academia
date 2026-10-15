package com.funcode.academy

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de humo en un dispositivo o emulador real (no Robolectric): abre la app,
 * espera a que Inicio cargue el contenido empaquetado y abre la primera lección.
 * Detecta fallos que solo ocurren en Android real, como el de las expresiones
 * regulares que cerraba la app al abrirla.
 */
@RunWith(AndroidJUnit4::class)
class LaunchSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = rule.activity.getString(id)

    @Test
    fun laAppAbreInicioYLaPrimeraLeccionSinCerrarse() {
        // El botón «Continuar» solo aparece cuando el contenido y el progreso ya se leyeron.
        rule.waitUntil(timeoutMillis = 20_000) {
            rule.onAllNodesWithText(text(R.string.home_continue_button)).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(text(R.string.home_continue_button)).performScrollTo().performClick()
        // La lección se abre (se ven sus objetivos) y la actividad sigue viva.
        rule.waitUntil(timeoutMillis = 20_000) {
            rule.onAllNodes(hasText(text(R.string.lesson_objectives_title))).fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitForIdle()
        assertFalse(rule.activity.isFinishing)
    }
}
