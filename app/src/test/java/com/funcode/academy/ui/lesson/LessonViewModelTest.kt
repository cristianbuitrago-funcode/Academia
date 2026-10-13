package com.funcode.academy.ui.lesson

import com.funcode.academy.domain.lesson.Answer
import com.funcode.academy.domain.lesson.AnswerResult
import com.funcode.academy.domain.lesson.AssessmentScore
import com.funcode.academy.testing.DemoContent
import com.funcode.academy.testing.FakeContentRepository
import com.funcode.academy.testing.FakeProgressRepository
import com.funcode.academy.testing.FakeSettingsRepository
import com.funcode.academy.ui.screens.lesson.LessonUiState
import com.funcode.academy.ui.screens.lesson.LessonViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Flujo completo de una lección a nivel de ViewModel, con el contenido real (JVM, sin Android). */
@OptIn(ExperimentalCoroutinesApi::class)
class LessonViewModelTest {

    private val lesson = DemoContent.lesson()
    private val progress = FakeProgressRepository()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private val assessment = DemoContent.assessment()

    private fun viewModel(id: String = lesson.id) =
        LessonViewModel(id, FakeContentRepository(listOf(lesson, assessment)), progress, FakeSettingsRepository())

    /** Hace la evaluación respondiendo bien a las [correctCount] primeras preguntas y mal al resto. */
    private fun takeAssessment(vm: LessonViewModel, correctCount: Int) {
        vm.startExercises()
        assessment.exercises.forEachIndexed { index, exercise ->
            val content = exercise.content
            vm.updateDraft(if (index < correctCount) DemoContent.correctAnswerFor(content) else DemoContent.wrongAnswerFor(content))
            vm.check()
            if (index < correctCount) {
                assertEquals(exercise.id, AnswerResult.Correct, vm.exercising.feedback)
            } else {
                assertTrue(exercise.id, vm.exercising.feedback is AnswerResult.Incorrect)
            }
            vm.next()
        }
    }

    private val LessonViewModel.exercising get() = uiState.value as LessonUiState.Exercising

    @Test
    fun cargaLaIntroduccionConObjetivosYTeoria() {
        val state = viewModel().uiState.value
        assertTrue(state is LessonUiState.Intro)
        state as LessonUiState.Intro
        assertEquals(lesson.objectives, state.lesson.objectives)
        assertTrue(state.sections.isNotEmpty())
    }

    @Test
    fun unaLeccionInexistenteMuestraNoEncontrada() =
        assertEquals(LessonUiState.NotFound, viewModel("java.m99.l99").uiState.value)

    @Test
    fun recorridoCompletoAlPrimerIntentoGeneraElResumen() {
        val vm = viewModel()
        vm.startExercises()
        lesson.exercises.forEach { exercise ->
            assertEquals(exercise.id, vm.exercising.session.currentExercise.id)
            vm.updateDraft(DemoContent.correctAnswerFor(exercise.content))
            vm.check()
            assertEquals(exercise.id, AnswerResult.Correct, vm.exercising.feedback)
            vm.next()
        }
        val finished = vm.uiState.value as LessonUiState.Finished
        assertEquals(7, finished.summary.total)
        assertEquals(7, finished.summary.solvedFirstTry)
        assertEquals(0, finished.summary.unsolved)
        assertEquals(7, progress.attempts.size)
        assertTrue(progress.attempts.all { it.correct })
    }

    @Test
    fun fallarReintentarYUsarPistasQuedaRegistrado() {
        val vm = viewModel()
        vm.startExercises() // e01: verdadero o falso (la respuesta correcta es "verdadero")
        vm.revealHint()
        vm.updateDraft(Answer.TrueFalse(false))
        vm.check()
        assertTrue(vm.exercising.feedback is AnswerResult.Incorrect)
        vm.retry()
        assertEquals(null, vm.exercising.feedback)
        vm.updateDraft(Answer.TrueFalse(true))
        vm.check()
        assertEquals(AnswerResult.Correct, vm.exercising.feedback)
        assertEquals(listOf(false, true), progress.attempts.map { it.correct })
        assertEquals(listOf(1, 1), progress.attempts.map { it.hintsUsed })
    }

    @Test
    fun unaRespuestaIncompletaNoSeRegistraComoIntento() {
        val vm = viewModel()
        vm.startExercises()
        vm.check() // sin elegir nada
        assertTrue(vm.exercising.feedback is AnswerResult.Incomplete)
        assertTrue(progress.attempts.isEmpty())
    }

    @Test
    fun saltarUnEjercicioLoDejaSinResolverEnElResumen() {
        val vm = viewModel()
        vm.startExercises()
        repeat(lesson.exercises.size) { vm.next() }
        val summary = (vm.uiState.value as LessonUiState.Finished).summary
        assertEquals(7, summary.unsolved)
        assertTrue(progress.attempts.isEmpty())
    }

    @Test
    fun elOrdenInicialDeLasLineasNoEsElCorrecto() {
        val order = LessonViewModel.shuffledOrder("java.m01.l05.e05", 6)
        assertEquals((0..5).toList(), order.sorted())
        assertNotEquals((0..5).toList(), order)
    }

    @Test
    fun laEvaluacionCalculaElResultadoYLoGuardaUnaSolaVez() {
        val vm = viewModel(assessment.id)
        takeAssessment(vm, correctCount = 7)

        val summary = (vm.uiState.value as LessonUiState.Finished).summary
        assertEquals(AssessmentScore(correct = 7, total = 10), summary.assessmentScore)
        assertEquals(70, summary.assessmentScore?.percent)
        // Sin umbral aprobado (P6) no se decide si se aprueba.
        assertEquals(null, summary.assessmentPassed)
        assertEquals(listOf("java.m01" to AssessmentScore(7, 10)), progress.assessmentResults)

        vm.next() // pulsar de nuevo en el resumen no vuelve a guardar
        assertEquals(1, progress.assessmentResults.size)
    }

    @Test
    fun enLaEvaluacionNoSePuedeReintentarNiPedirPistas() {
        val vm = viewModel(assessment.id)
        vm.startExercises()
        assertEquals(false, vm.exercising.session.allowsRetries)
        vm.revealHint()
        assertEquals(0, vm.exercising.session.currentProgress.hintsRevealed)

        val first = assessment.exercises.first()
        vm.updateDraft(DemoContent.wrongAnswerFor(first.content))
        vm.check()
        assertTrue(vm.exercising.feedback is AnswerResult.Incorrect)
        // Cambiar la respuesta y comprobar otra vez no cuenta como un nuevo intento.
        vm.updateDraft(DemoContent.correctAnswerFor(first.content))
        vm.check()
        assertEquals(1, progress.attempts.size)
        assertEquals(false, progress.attempts.single().correct)
    }

    @Test
    fun unaPreguntaSinResponderCuentaComoFalloSinRegistrarIntento() {
        val vm = viewModel(assessment.id)
        vm.startExercises()
        vm.check() // sin responder: incompleta, no se registra
        assertTrue(vm.exercising.feedback is AnswerResult.Incomplete)
        repeat(assessment.exercises.size) { vm.next() }

        val summary = (vm.uiState.value as LessonUiState.Finished).summary
        assertEquals(AssessmentScore(correct = 0, total = 10), summary.assessmentScore)
        assertTrue(progress.attempts.isEmpty())
        assertEquals(listOf("java.m01" to AssessmentScore(0, 10)), progress.assessmentResults)
    }

    @Test
    fun unaLeccionNormalNoGuardaResultadoDeEvaluacion() {
        val vm = viewModel()
        vm.startExercises()
        repeat(lesson.exercises.size) { vm.next() }
        assertEquals(null, (vm.uiState.value as LessonUiState.Finished).summary.assessmentScore)
        assertTrue(progress.assessmentResults.isEmpty())
    }
}
