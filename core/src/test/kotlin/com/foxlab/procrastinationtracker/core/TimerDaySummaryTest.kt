package com.foxlab.procrastinationtracker.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The productive vs. procrastinated report. The rule worth protecting here is that an interrupted
 * block still counts towards the *time* but never towards the *cycle count* -- the distinction the
 * whole summary exists for.
 */
class TimerDaySummaryTest {

    private fun session(
        mode: TimerMode = TimerMode.FIFTY_TWO_SEVENTEEN,
        phase: Phase = Phase.FOCUS,
        minutes: Long,
        completed: Boolean = true
    ) = Session(
        mode = mode,
        phase = phase,
        startTimeMillis = 0,
        endTimeMillis = minutes * 60_000L,
        completedFully = completed
    )

    @Test
    fun `an empty day reports nothing`() {
        val summary = TimerDaySummary.from(emptyList())

        assertFalse(summary.hasAnything)
        assertEquals(0, summary.completedFocusBlocks)
        assertEquals(0L, summary.focusMillis)
    }

    @Test
    fun `finished focus blocks count as time and as cycles`() {
        val summary = TimerDaySummary.from(
            listOf(session(minutes = 52), session(minutes = 52))
        )

        assertEquals(2, summary.completedFocusBlocks)
        assertEquals(104 * 60_000L, summary.focusMillis)
        assertTrue(summary.hasAnything)
    }

    @Test
    fun `an interrupted block keeps its time but not its cycle`() {
        val summary = TimerDaySummary.from(
            listOf(
                session(minutes = 52),
                session(minutes = 13, completed = false)
            )
        )

        assertEquals(1, summary.completedFocusBlocks)
        assertEquals(65 * 60_000L, summary.focusMillis)
    }

    @Test
    fun `breaks are counted apart from focus`() {
        val summary = TimerDaySummary.from(
            listOf(
                session(minutes = 52),
                session(phase = Phase.SHORT_BREAK, minutes = 17),
                session(phase = Phase.LONG_BREAK, minutes = 30)
            )
        )

        assertEquals(1, summary.completedFocusBlocks)
        assertEquals(2, summary.completedBreaks)
        assertEquals(52 * 60_000L, summary.focusMillis)
        assertEquals(47 * 60_000L, summary.breakMillis)
    }

    @Test
    fun `finished focus blocks are broken down by mode`() {
        val summary = TimerDaySummary.from(
            listOf(
                session(mode = TimerMode.POMODORO, minutes = 25),
                session(mode = TimerMode.POMODORO, minutes = 25),
                session(mode = TimerMode.FORTY_FIVE_FIFTEEN, minutes = 45),
                session(mode = TimerMode.FORTY_FIVE_FIFTEEN, minutes = 20, completed = false)
            )
        )

        assertEquals(
            mapOf(TimerMode.POMODORO to 2, TimerMode.FORTY_FIVE_FIFTEEN to 1),
            summary.focusBlocksByMode
        )
    }

    @Test
    fun `a day of only interrupted blocks still has something to show`() {
        val summary = TimerDaySummary.from(listOf(session(minutes = 3, completed = false)))

        assertTrue(summary.hasAnything)
        assertEquals(0, summary.completedFocusBlocks)
    }
}
