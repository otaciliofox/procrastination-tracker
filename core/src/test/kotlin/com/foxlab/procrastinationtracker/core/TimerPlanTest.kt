package com.foxlab.procrastinationtracker.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `sanitized()` is the guard between a custom plan typed by the user and the engine. Everything
 * the dialog might let through has to come out inside the supported range.
 */
class TimerPlanTest {

    private fun custom(
        focus: Int = 40,
        shortBreak: Int = 10,
        longBreak: Int = 20,
        cycles: Int = 4
    ) = TimerPlan(TimerMode.CUSTOM, focus, shortBreak, longBreak, cycles)

    @Test
    fun `preset modes copy their own numbers`() {
        val plan = TimerPlan.of(TimerMode.FIFTY_TWO_SEVENTEEN)

        assertEquals(52, plan.focusMinutes)
        assertEquals(17, plan.shortBreakMinutes)
        assertEquals("52/17", plan.label)
    }

    @Test
    fun `minutes above the ceiling are clamped`() {
        val plan = custom(focus = 999, shortBreak = 500, longBreak = 181).sanitized()

        assertEquals(TimerPlan.MAX_MINUTES, plan.focusMinutes)
        assertEquals(TimerPlan.MAX_MINUTES, plan.shortBreakMinutes)
        assertEquals(TimerPlan.MAX_MINUTES, plan.longBreakMinutes)
    }

    @Test
    fun `zero and negative minutes are raised to the floor`() {
        val plan = custom(focus = 0, shortBreak = -5, longBreak = -1).sanitized()

        assertEquals(TimerPlan.MIN_MINUTES, plan.focusMinutes)
        assertEquals(TimerPlan.MIN_MINUTES, plan.shortBreakMinutes)
        assertEquals(TimerPlan.MIN_MINUTES, plan.longBreakMinutes)
    }

    @Test
    fun `cycle count is clamped into the supported range`() {
        assertEquals(TimerPlan.MIN_CYCLES, custom(cycles = 1).sanitized().focusSessionsBeforeLongBreak)
        assertEquals(TimerPlan.MAX_CYCLES, custom(cycles = 50).sanitized().focusSessionsBeforeLongBreak)
    }

    @Test
    fun `the disabled sentinel survives sanitising`() {
        val plan = custom(cycles = TimerPlan.NO_LONG_BREAK).sanitized()

        assertEquals(TimerPlan.NO_LONG_BREAK, plan.focusSessionsBeforeLongBreak)
        assertFalse(plan.hasLongBreak)
    }

    @Test
    fun `52-17 has no long break and pomodoro does`() {
        assertFalse(TimerPlan.of(TimerMode.FIFTY_TWO_SEVENTEEN).hasLongBreak)
        assertFalse(TimerPlan.of(TimerMode.FORTY_FIVE_FIFTEEN).hasLongBreak)
        assertTrue(TimerPlan.of(TimerMode.POMODORO).hasLongBreak)
    }

    @Test
    fun `an already valid plan is left untouched`() {
        val plan = custom(focus = 40, shortBreak = 10, longBreak = 20, cycles = 4)

        assertEquals(plan, plan.sanitized())
    }
}
