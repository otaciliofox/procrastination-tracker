package com.foxlab.procrastinationtracker.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The engine takes time as a parameter (`tick(deltaMillis)`) instead of reading a clock, so a
 * whole day of cycles runs here in microseconds. That is the entire reason these rules are worth
 * asserting at this level rather than through the UI: proving the 52/17 cycle on a device would
 * mean waiting 52 real minutes per block.
 */
class TimerEngineTest {

    private fun TimerEngine.runFor(
        millis: Long,
        step: Long = 1000
    ): List<TimerEngine.Event.PhaseCompleted> {
        val events = mutableListOf<TimerEngine.Event.PhaseCompleted>()
        var elapsed = 0L
        while (elapsed < millis) {
            val event = tick(step)
            if (event is TimerEngine.Event.PhaseCompleted) events += event
            elapsed += step
        }
        return events
    }

    @Test
    fun `starts in focus with the full focus duration`() {
        val engine = TimerEngine(TimerMode.FIFTY_TWO_SEVENTEEN)

        assertEquals(Phase.FOCUS, engine.phase)
        assertEquals(52 * 60_000L, engine.remainingMillis)
        assertFalse(engine.isRunning)
    }

    @Test
    fun `does not advance while paused`() {
        val engine = TimerEngine(TimerMode.POMODORO)
        val before = engine.remainingMillis

        val event = engine.tick(60_000)

        assertIs<TimerEngine.Event.None>(event)
        assertEquals(before, engine.remainingMillis)
    }

    @Test
    fun `stops advancing after pause`() {
        val engine = TimerEngine(TimerMode.POMODORO)
        engine.start(nowMillis = 0)
        engine.runFor(60_000)
        val afterOneMinute = engine.remainingMillis

        engine.pause()
        engine.runFor(60_000)

        assertEquals(afterOneMinute, engine.remainingMillis)
    }

    @Test
    fun `focus block hands over to a short break`() {
        val engine = TimerEngine(TimerMode.FIFTY_TWO_SEVENTEEN)
        engine.start(nowMillis = 0)

        val events = engine.runFor(52 * 60_000L)

        assertEquals(1, events.size)
        val completed = events.single()
        assertEquals(Phase.FOCUS, completed.finishedPhase)
        assertEquals(Phase.SHORT_BREAK, completed.nextPhase)
        assertEquals(17 * 60_000L, engine.remainingMillis)
    }

    @Test
    fun `52-17 never produces a long break`() {
        val engine = TimerEngine(TimerMode.FIFTY_TWO_SEVENTEEN)
        engine.start(nowMillis = 0)

        // Six full focus+break rounds -- four times past where Pomodoro would insert a long break.
        val events = engine.runFor(6 * (52 + 17) * 60_000L)

        assertTrue(events.none { it.nextPhase == Phase.LONG_BREAK })
    }

    @Test
    fun `pomodoro inserts the long break after the fourth focus block`() {
        val engine = TimerEngine(TimerMode.POMODORO)
        engine.start(nowMillis = 0)

        // Four focus blocks with three short breaks between them: the moment the long break is due,
        // and not a tick further, so `remainingMillis` below reads the long break at full length.
        val events = engine.runFor((4 * 25 + 3 * 5) * 60_000L)

        val focusHandovers = events.filter { it.finishedPhase == Phase.FOCUS }
        assertEquals(4, focusHandovers.size)
        assertEquals(
            listOf(Phase.SHORT_BREAK, Phase.SHORT_BREAK, Phase.SHORT_BREAK, Phase.LONG_BREAK),
            focusHandovers.map { it.nextPhase }
        )
        assertEquals(30 * 60_000L, engine.remainingMillis)
    }

    @Test
    fun `the cadence repeats after the long break`() {
        val engine = TimerEngine(TimerMode.POMODORO)
        engine.start(nowMillis = 0)

        // Two complete sets: 4 focus blocks, a long break, then 4 more.
        val firstSet = 4 * (25 + 5) * 60_000L
        val events = engine.runFor(firstSet + 30 * 60_000L + 4 * (25 + 5) * 60_000L)

        val longBreaks = events.count { it.nextPhase == Phase.LONG_BREAK }
        assertEquals(2, longBreaks)
    }

    @Test
    fun `elapsed in phase reports what an interrupted block already spent`() {
        val engine = TimerEngine(TimerMode.FIFTY_TWO_SEVENTEEN)
        engine.start(nowMillis = 0)

        engine.runFor(10 * 60_000L)

        assertEquals(10 * 60_000L, engine.elapsedInPhaseMillis)
    }

    @Test
    fun `changing mode resets the machine`() {
        val engine = TimerEngine(TimerMode.POMODORO)
        engine.start(nowMillis = 0)
        engine.runFor(4 * (25 + 5) * 60_000L)

        engine.changeMode(TimerMode.FORTY_FIVE_FIFTEEN)

        assertEquals(Phase.FOCUS, engine.phase)
        assertEquals(45 * 60_000L, engine.remainingMillis)
        assertEquals(0, engine.focusStreak)
        assertFalse(engine.isRunning)
    }

    @Test
    fun `a custom plan drives the same machine with different numbers`() {
        val plan = TimerPlan(
            mode = TimerMode.CUSTOM,
            focusMinutes = 10,
            shortBreakMinutes = 2,
            longBreakMinutes = 15,
            focusSessionsBeforeLongBreak = 3
        )
        val engine = TimerEngine(plan)
        engine.start(nowMillis = 0)

        val events = engine.runFor(3 * (10 + 2) * 60_000L)

        val focusHandovers = events.filter { it.finishedPhase == Phase.FOCUS }
        assertEquals(3, focusHandovers.size)
        assertEquals(Phase.LONG_BREAK, focusHandovers.last().nextPhase)
    }

    @Test
    fun `start is ignored while already running`() {
        val engine = TimerEngine(TimerMode.POMODORO)
        engine.start(nowMillis = 1_000)

        engine.start(nowMillis = 9_999)

        assertEquals(1_000, engine.phaseStartTimeMillis)
    }
}
