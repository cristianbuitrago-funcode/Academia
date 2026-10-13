package com.funcode.academy.ui.lesson

import com.funcode.academy.domain.lesson.Answer
import com.funcode.academy.domain.lesson.AnswerResult
import com.funcode.academy.domain.lesson.AssessmentScore
import com.funcode.academy.domain.lesson.LessonKind
import com.funcode.academy.domain.model.Audience
import com.funcode.academy.domain.settings.LearningPreferences
import com.funcode.academy.domain.progress.ExerciseAttempt
import com.funcode.academy.domain.progress.XpSource
import com.funcode.academy.domain.progress.XpSourceType
import com.funcode.academy.domain.rpg.Achievement
import com.funcode.academy.domain.rpg.RewardEngine
import com.funcode.academy.testing.DemoContent
import com.funcode.academy.testing.FakeProgressRepository
import com.funcode.academy.testing.FakeSettingsRepository
import com.funcode.academy.ui.screens.lesson.LessonUiState
import com.funcode.academy.ui.screens.lesson.LessonViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

    private val content = DemoContent.repository()
    private val engine = RewardEngine(content, progress)

    private fun viewModel(id: String = lesson.id) =
        LessonViewModel(id, content, progress, FakeSettingsRepository(), engine)

    private fun solveAll(vm: LessonViewModel) {
        vm.startExercises()
        lesson.exercises.forEach { exercise ->
            vm.updateDraft(DemoContent.correctAnswerFor(exercise.content))
            vm.check()
            vm.next()
        }
    }

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
        // Umbral del 80 % (P6): 7 de 10 no aprueba.
        assertEquals(false, summary.assessmentPassed)
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

    @Test
    fun unAciertoConcedeElXpDelEjercicioYLoAvisa() {
        val vm = viewModel()
        vm.startExercises() // e01, dificultad 1
        vm.updateDraft(DemoContent.correctAnswerFor(lesson.exercises.first().content))
        vm.check()
        val notice = vm.rewardNotice.value!!
        assertEquals(10, notice.xpGained)
        assertEquals(false, notice.alreadyRewarded)
        assertEquals(listOf(XpSource(XpSourceType.EXERCISE, "java.m01.l05.e01")), progress.ledgerEntries.map { it.source })
    }

    @Test
    fun unFalloNoConcedeXp() {
        val vm = viewModel()
        vm.startExercises()
        vm.updateDraft(Answer.TrueFalse(false))
        vm.check()
        assertEquals(null, vm.rewardNotice.value)
        assertTrue(progress.ledgerEntries.isEmpty())
    }

    @Test
    fun lasPistasReducenElXpDelPrimerAcierto() {
        val vm = viewModel()
        vm.startExercises()
        vm.revealHint()
        vm.updateDraft(DemoContent.correctAnswerFor(lesson.exercises.first().content))
        vm.check()
        assertEquals(7, vm.rewardNotice.value?.xpGained)
    }

    @Test
    fun completarLaLeccionDaBonificacionSubeDeNivelYDesbloqueaLogros() {
        val vm = viewModel()
        solveAll(vm)
        // 10+10+15+15+20+20+25 = 115 por ejercicios + 20 por la lección = 135 → nivel 2
        assertEquals(135, vm.sessionXp.value)
        val notice = vm.rewardNotice.value!!
        assertTrue(notice.lessonCompleted)
        assertEquals(2, notice.newLevel)
        assertEquals(setOf(Achievement.FIRST_LESSON, Achievement.LEVEL_2), notice.achievements.toSet())
        assertEquals(setOf("first_lesson", "level_2"), progress.unlockedAchievements)
    }

    @Test
    fun repetirLaLeccionNoDaMasXpNiDuplicaLogros() {
        solveAll(viewModel())
        val entries = progress.ledgerEntries.size

        val again = viewModel()
        again.startExercises()
        again.updateDraft(DemoContent.correctAnswerFor(lesson.exercises.first().content))
        again.check()
        assertEquals(true, again.rewardNotice.value?.alreadyRewarded)
        assertEquals(0, again.rewardNotice.value?.xpGained)
        solveAll(again)
        assertEquals(0, again.sessionXp.value)
        assertEquals(entries, progress.ledgerEntries.size)
        assertEquals(2, progress.unlockedAchievements.size)
    }

    @Test
    fun aprobarLaEvaluacionDaSuXpUnaSolaVez() {
        takeAssessment(viewModel(assessment.id), correctCount = 8)
        val summary = (viewModel(assessment.id).also { takeAssessment(it, correctCount = 10) }.uiState.value as LessonUiState.Finished).summary
        assertEquals(true, summary.assessmentPassed)
        val assessmentAwards = progress.ledgerEntries.filter { it.source.type == XpSourceType.ASSESSMENT }
        assertEquals(listOf(100), assessmentAwards.map { it.amount })
        // Las preguntas de la evaluación no conceden XP por separado.
        assertTrue(progress.ledgerEntries.none { it.source.type == XpSourceType.EXERCISE })
        assertTrue("first_assessment_passed" in progress.unlockedAchievements)
    }

    @Test
    fun suspenderLaEvaluacionNoDaXp() {
        val vm = viewModel(assessment.id)
        takeAssessment(vm, correctCount = 7)
        assertEquals(null, vm.rewardNotice.value)
        assertTrue(progress.ledgerEntries.isEmpty())
    }

    private val challenge = DemoContent.lesson("java.m01.reto")

    /** Guarda como resueltos todos los ejercicios de las lecciones del módulo. */
    private fun completeAllLessons() = runBlocking {
        DemoContent.allLessons().filter { it.kind == LessonKind.LESSON }.flatMap { it.exercises }
            .forEach { progress.recordAttempt(ExerciseAttempt(it.id, correct = true, hintsUsed = 0)) }
        engine.sync()
    }

    @Test
    fun elRetoEstaCerradoHastaCompletarLasLecciones() {
        val state = viewModel(challenge.id).uiState.value
        assertEquals(LessonUiState.Locked(challenge, lessonsRemaining = 7), state)
        val vm = viewModel(challenge.id)
        vm.startExercises() // no hace nada mientras está cerrado
        assertTrue(vm.uiState.value is LessonUiState.Locked)
    }

    @Test
    fun superarElRetoDa50XpYSuLogroUnaSolaVez() {
        completeAllLessons()
        val vm = viewModel(challenge.id)
        assertTrue(vm.uiState.value is LessonUiState.Intro)
        vm.startExercises()
        assertEquals(true, vm.exercising.session.allowsRetries)
        challenge.exercises.forEachIndexed { i, step ->
            vm.updateDraft(DemoContent.correctAnswerFor(step.content))
            vm.check()
            if (i < challenge.exercises.lastIndex) {
                // Los pasos no dan XP ni avisan de "ya recompensado".
                assertEquals(null, vm.rewardNotice.value)
            }
            vm.next()
        }
        // 50 XP del proyecto + 10 de la misión del proyecto (sus requisitos ya estaban cumplidos).
        assertEquals(60, vm.sessionXp.value)
        val notice = vm.rewardNotice.value!!
        assertTrue(notice.challengeCompleted)
        assertEquals(listOf("Proyecto guiado: tu tarjeta de presentación"), notice.completedMissions)
        assertTrue(Achievement.FIRST_CHALLENGE in notice.achievements)
        assertEquals(listOf(50), progress.ledgerEntries.filter { it.source.type == XpSourceType.CHALLENGE }.map { it.amount })

        val again = viewModel(challenge.id)
        again.startExercises()
        challenge.exercises.forEach { step ->
            again.updateDraft(DemoContent.correctAnswerFor(step.content))
            again.check()
            again.next()
        }
        assertEquals(0, again.sessionXp.value)
        assertEquals(1, progress.ledgerEntries.count { it.source.type == XpSourceType.CHALLENGE })
    }

    @Test
    fun laVarianteElegidaCambiaLaTeoriaPeroNoLosObjetivosNiLosEjercicios() {
        val l01 = DemoContent.lesson("java.m01.l01")
        val intros = Audience.entries.associateWith { audience ->
            val vm = LessonViewModel(l01.id, content, progress, FakeSettingsRepository(audience), engine)
            vm.uiState.value as LessonUiState.Intro
        }
        assertEquals(Audience.entries.size, intros.values.map { it.sections }.toSet().size)
        intros.forEach { (audience, intro) ->
            assertEquals(l01.sectionsFor(audience), intro.sections)
            assertEquals(l01.objectives, intro.lesson.objectives)
            assertEquals(l01.exercises, intro.lesson.exercises)
        }
    }

    @Test
    fun laPreferenciaDePistasSeExponeALaPantalla() = runBlocking {
        val settings = FakeSettingsRepository(LearningPreferences(hintsEnabled = false))
        val vm = LessonViewModel(lesson.id, content, progress, settings, engine)
        assertEquals(false, vm.hintsEnabled.first { !it })
    }
}
