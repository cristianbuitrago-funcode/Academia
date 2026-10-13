package com.funcode.academy.ui.mission

import com.funcode.academy.domain.mission.MissionStatus
import com.funcode.academy.domain.progress.ExerciseAttempt
import com.funcode.academy.domain.rpg.RewardEngine
import com.funcode.academy.testing.DemoContent
import com.funcode.academy.testing.FakeProgressRepository
import com.funcode.academy.ui.screens.mission.MissionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pantalla de misiones a nivel de ViewModel, con el contenido real (JVM). */
@OptIn(ExperimentalCoroutinesApi::class)
class MissionsViewModelTest {

    private val progress = FakeProgressRepository()
    private val content = DemoContent.repository()
    private val engine = RewardEngine(content, progress)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun muestraLasMisionesConSuEstadoYLosTitulosDeSusActividades() = runTest {
        val vm = MissionsViewModel(content, progress, engine)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        val state = vm.state.filterNotNull().first()
        assertEquals(7, state.missions.size)
        assertEquals(MissionStatus.AVAILABLE, state.missions.first().status)
        assertTrue(state.missions.drop(1).all { it.status == MissionStatus.LOCKED })
        assertEquals("¿Qué es programar?", state.activityTitles["java.m01.l01"])
        job.cancel()
    }

    @Test
    fun elEstadoSeActualizaConLosResultadosGuardados() = runTest {
        val vm = MissionsViewModel(content, progress, engine)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        progress.recordAttempt(ExerciseAttempt("java.m01.l01.e01", correct = true, hintsUsed = 0))
        val state = vm.state.filterNotNull().first { it.missions.first().status == MissionStatus.IN_PROGRESS }
        assertEquals(1, state.find("java.m01.mi01")!!.tasks.first().current)
        job.cancel()
    }
}
