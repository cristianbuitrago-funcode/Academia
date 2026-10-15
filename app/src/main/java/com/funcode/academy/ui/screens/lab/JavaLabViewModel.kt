package com.funcode.academy.ui.screens.lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.funcode.academy.domain.runner.CodeRunner
import com.funcode.academy.domain.runner.ProgramEvaluation
import com.funcode.academy.domain.runner.RunRequest
import com.funcode.academy.domain.runner.RunResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Laboratorio de Java (PROTOTIPO Fase 8). No está integrado en las lecciones: no registra
 * intentos ni da XP. El código solo vive en memoria mientras la pantalla está abierta.
 */
class JavaLabViewModel(private val runner: CodeRunner) : ViewModel() {

    data class UiState(
        val code: String = TEMPLATE,
        val running: Boolean = false,
        val result: RunResult? = null,
        /** Comparación con la salida esperada del ejercicio de prueba (solo informativa). */
        val outcome: ProgramEvaluation.Outcome? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var job: Job? = null

    fun onCodeChange(code: String) = _state.update { it.copy(code = code) }

    fun onRun() {
        if (_state.value.running) return
        val code = _state.value.code
        _state.update { it.copy(running = true, result = null, outcome = null) }
        job = viewModelScope.launch {
            val result = try {
                runner.run(RunRequest(code))
            } catch (e: CancellationException) {
                _state.update { it.copy(running = false, result = RunResult.Cancelled(), outcome = null) }
                throw e
            }
            _state.update { it.copy(running = false, result = result, outcome = ProgramEvaluation.evaluate(result, EXPECTED)) }
        }
    }

    fun onCancel() {
        job?.cancel()
        _state.update { it.copy(running = false, result = RunResult.Cancelled(), outcome = null) }
    }

    fun onReset() = _state.update { UiState() }

    companion object {
        /** Ejercicio de prueba: imprimir el resultado de una operación. */
        val EXPECTED = listOf("Hola, Funcode\n7 + 5 = 12")

        val TEMPLATE = """
            public class Main {
                public static void main(String[] args) {
                    int a = 7;
                    int b = 5;
                    System.out.println("Hola, Funcode");
                    System.out.println(a + " + " + b + " = " + (a + b));
                }
            }
        """.trimIndent()

        fun factory(runner: CodeRunner) = viewModelFactory { initializer { JavaLabViewModel(runner) } }
    }
}
