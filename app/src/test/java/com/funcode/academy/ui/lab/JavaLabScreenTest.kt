package com.funcode.academy.ui.lab

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.funcode.academy.R
import com.funcode.academy.domain.runner.CodeRunner
import com.funcode.academy.domain.runner.Diagnostic
import com.funcode.academy.domain.runner.Limit
import com.funcode.academy.domain.runner.RunRequest
import com.funcode.academy.domain.runner.RunResult
import com.funcode.academy.domain.runner.RunnerCapabilities
import com.funcode.academy.ui.screens.lab.JavaLabRoute
import com.funcode.academy.ui.screens.lab.JavaLabViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pantalla del laboratorio con un ejecutor FALSO (Robolectric no ejecuta Java real):
 * comprueba que cada estado dice la verdad sobre lo que ocurrió. La ejecución real se
 * prueba en el emulador (JavaRunnerInstrumentedTest).
 */
@RunWith(AndroidJUnit4::class)
class JavaLabScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun text(@StringRes id: Int, vararg args: Any) = rule.activity.getString(id, *args)

    private class ScriptedRunner(private val result: RunResult) : CodeRunner {
        override val capabilities = RunnerCapabilities(canExecute = true)
        val requests = mutableListOf<RunRequest>()
        override suspend fun run(request: RunRequest): RunResult {
            requests += request
            return result
        }
    }

    private fun open(runner: CodeRunner) = rule.setContent { JavaLabRoute(runner) }

    private fun runIt() = rule.onNode(hasText(text(R.string.lab_run)) and hasClickAction()).performScrollTo().performClick()

    private fun assertShown(value: String) {
        rule.waitUntil(5_000) { rule.onAllNodes(hasText(value, substring = true)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasText(value, substring = true)).performScrollTo().assertIsDisplayed()
    }

    private fun assertNotShown(value: String) =
        assertEquals(0, rule.onAllNodes(hasText(value, substring = true)).fetchSemanticsNodes().size)

    @Test
    fun salidaRealCorrectaMuestraEjecucionYComparacion() {
        val runner = ScriptedRunner(RunResult.Completed("Hola, Funcode\n7 + 5 = 12\n"))
        open(runner)
        runIt()
        assertShown(text(R.string.lab_status_completed))
        assertShown("7 + 5 = 12")
        assertShown(text(R.string.lab_check_ok))
        assertEquals(JavaLabViewModel.TEMPLATE, runner.requests.single().source)
    }

    @Test
    fun errorDeCompilacionNoDiceQueSeEjecuto() {
        open(ScriptedRunner(RunResult.CompilationFailed(listOf(Diagnostic(5, "Syntax error, insert \";\"")))))
        runIt()
        assertShown(text(R.string.lab_status_compile_error))
        assertShown(text(R.string.lab_diagnostic_line, 5, "Syntax error, insert \";\""))
        assertNotShown(text(R.string.lab_status_completed))
        assertNotShown(text(R.string.lab_output_title))
    }

    @Test
    fun errorDeEjecucionYLimites() {
        open(ScriptedRunner(RunResult.RuntimeError("ArithmeticException", "divide by zero", 4, "antes\n")))
        runIt()
        assertShown(text(R.string.lab_status_runtime_error, "ArithmeticException", "4"))
        assertShown("antes")
    }

    @Test
    fun tiempoAgotado() {
        open(ScriptedRunner(RunResult.LimitExceeded(Limit.TIME, "")))
        runIt()
        assertShown(text(R.string.lab_status_time_limit))
        assertNotShown(text(R.string.lab_check_ok))
    }

    @Test
    fun ejecutorNoDisponibleNoCulpaAlEstudiante() {
        open(ScriptedRunner(RunResult.Unavailable("D8 falló")))
        runIt()
        assertShown(text(R.string.lab_status_unavailable, "D8 falló"))
        assertNotShown(text(R.string.lab_status_completed))
    }
}
