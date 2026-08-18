package com.foxlab.procrastinationtracker.viewmodel

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.foxlab.procrastinationtracker.core.Phase
import com.foxlab.procrastinationtracker.core.Session
import com.foxlab.procrastinationtracker.core.TimerMode
import com.foxlab.procrastinationtracker.data.AppDatabase
import com.foxlab.procrastinationtracker.data.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * This test is the whole point of the dependency injection work.
 *
 * The ViewModel used to read its repository through `application as ProcrastinationTrackerApp`,
 * which meant instantiating it required that exact Application to be running -- and that
 * Application opens the real on-disk database and starts a sync loop. Constructor injection makes
 * it an ordinary object: hand it a repository and assert what it exposes.
 *
 * The repository here is the real one over an in-memory Room database, not a fake, so the SQL and
 * the entity mapping take part in the assertions too.
 *
 * Note `runCurrent()` rather than `advanceUntilIdle()`: the repository watches for midnight with a
 * `while (true) { emit(...); delay(60_000) }` flow, so advancing the virtual clock until idle
 * never returns. `runCurrent()` drains what is already scheduled, which is all these assertions
 * need.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class HistoryViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SessionRepository
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        // Room runs queries and fires invalidation on its own executor by default, so a Flow
        // emission would land after the test had already read the state. Pointing both executors
        // at the test dispatcher puts the database on the same virtual clock as the ViewModel.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .setQueryExecutor(dispatcher.asExecutor())
            .setTransactionExecutor(dispatcher.asExecutor())
            .build()
        repository = SessionRepository(database.sessionDao())
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun session(
        phase: Phase,
        minutes: Long,
        completed: Boolean = true,
        startedAt: Long = System.currentTimeMillis()
    ) = Session(
        mode = TimerMode.FIFTY_TWO_SEVENTEEN,
        phase = phase,
        startTimeMillis = startedAt,
        endTimeMillis = startedAt + minutes * 60_000L,
        completedFully = completed
    )

    @Test
    fun `starts empty before anything is recorded`() = runTest(dispatcher) {
        val viewModel = HistoryViewModel(repository)
        backgroundScope.launchCollecting(viewModel)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(emptyList<Session>(), state.today)
        assertEquals(0L, state.focusedTodayMillis)
        assertEquals(0, state.summary.completedFocusBlocks)
    }

    @Test
    fun `a recorded session reaches the screen state`() = runTest(dispatcher) {
        repository.record(session(Phase.FOCUS, minutes = 52))

        val viewModel = HistoryViewModel(repository)
        backgroundScope.launchCollecting(viewModel)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(1, state.today.size)
        assertEquals(52 * 60_000L, state.focusedTodayMillis)
        assertEquals(1, state.allHistory.size)
    }

    @Test
    fun `focus and break time are reported apart`() = runTest(dispatcher) {
        repository.record(session(Phase.FOCUS, minutes = 52))
        repository.record(session(Phase.SHORT_BREAK, minutes = 17))
        repository.record(session(Phase.FOCUS, minutes = 52))

        val viewModel = HistoryViewModel(repository)
        backgroundScope.launchCollecting(viewModel)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(104 * 60_000L, state.focusedTodayMillis)
        assertEquals(17 * 60_000L, state.procrastinatedTodayMillis)
        assertEquals(2, state.summary.completedFocusBlocks)
    }

    @Test
    fun `an interrupted block adds time but not a cycle`() = runTest(dispatcher) {
        repository.record(session(Phase.FOCUS, minutes = 52))
        repository.record(session(Phase.FOCUS, minutes = 13, completed = false))

        val viewModel = HistoryViewModel(repository)
        backgroundScope.launchCollecting(viewModel)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(65 * 60_000L, state.focusedTodayMillis)
        assertEquals(1, state.summary.completedFocusBlocks)
    }

    @Test
    fun `yesterday counts as history but not as today`() = runTest(dispatcher) {
        val yesterday = System.currentTimeMillis() - 36 * 60 * 60 * 1000L
        repository.record(session(Phase.FOCUS, minutes = 52, startedAt = yesterday))
        repository.record(session(Phase.FOCUS, minutes = 25))

        val viewModel = HistoryViewModel(repository)
        backgroundScope.launchCollecting(viewModel)
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("only today's block is today", 1, state.today.size)
        assertEquals("both blocks are history", 2, state.allHistory.size)
        assertEquals(25 * 60_000L, state.focusedTodayMillis)
    }
}

/**
 * `uiState` is a `WhileSubscribed` StateFlow, so it stays at its initial value until something
 * collects it. Every test needs a live subscriber before reading `.value`.
 */
private fun CoroutineScope.launchCollecting(viewModel: HistoryViewModel) {
    launch { viewModel.uiState.collect { } }
}
