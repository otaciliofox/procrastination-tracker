package com.foxlab.procrastinationtracker.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Both apps format time through these three functions, so a change here is visible on the phone
 * and on the watch at once. The negative cases matter: a countdown crossing zero between ticks
 * must never render "-1:-40" on screen.
 */
class TimeFormatTest {

    @Test
    fun `countdown renders as zero-padded minutes and seconds`() {
        assertEquals("52:00", (52 * 60_000L).toClockString())
        assertEquals("04:59", (4 * 60_000L + 59_000L).toClockString())
        assertEquals("00:00", 0L.toClockString())
    }

    @Test
    fun `countdown never renders a negative clock`() {
        assertEquals("00:00", (-5_000L).toClockString())
    }

    @Test
    fun `countdown truncates sub-second remainders`() {
        assertEquals("00:01", 1_999L.toClockString())
    }

    @Test
    fun `stopwatch drops the hour until there is one`() {
        assertEquals("0:00", 0L.toStopwatchString())
        assertEquals("4:07", (4 * 60_000L + 7_000L).toStopwatchString())
        assertEquals("59:59", (59 * 60_000L + 59_000L).toStopwatchString())
    }

    @Test
    fun `stopwatch adds the hour once it is reached`() {
        assertEquals("1:00:00", (60 * 60_000L).toStopwatchString())
        assertEquals("1:04:07", (64 * 60_000L + 7_000L).toStopwatchString())
        assertEquals("10:00:00", (10 * 60 * 60_000L).toStopwatchString())
    }

    @Test
    fun `stopwatch never renders a negative time`() {
        assertEquals("0:00", (-1_000L).toStopwatchString())
    }

    @Test
    fun `human readable totals drop the hour when there is none`() {
        assertEquals("0m", 0L.toHoursMinutesString())
        assertEquals("5m", (5 * 60_000L).toHoursMinutesString())
        assertEquals("1h 23m", (83 * 60_000L).toHoursMinutesString())
        assertEquals("2h 0m", (120 * 60_000L).toHoursMinutesString())
    }

    @Test
    fun `human readable totals ignore leftover seconds`() {
        assertEquals("1m", 119_000L.toHoursMinutesString())
    }
}
