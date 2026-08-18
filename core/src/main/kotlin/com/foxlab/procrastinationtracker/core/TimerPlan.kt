package com.foxlab.procrastinationtracker.core

/**
 * The concrete durations a run of the timer uses. For the three preset modes it is just a copy of
 * the enum's numbers; for [TimerMode.CUSTOM] it is whatever the user configured, which is why the
 * engine works on a plan instead of reading the enum directly.
 */
data class TimerPlan(
    val mode: TimerMode,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    /** How many focus blocks before the long break. [NO_LONG_BREAK] disables it. */
    val focusSessionsBeforeLongBreak: Int
) {
    val label: String get() = mode.label

    val hasLongBreak: Boolean get() = focusSessionsBeforeLongBreak in 1 until NO_LONG_BREAK

    fun sanitized(): TimerPlan = copy(
        focusMinutes = focusMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES),
        shortBreakMinutes = shortBreakMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES),
        longBreakMinutes = longBreakMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES),
        focusSessionsBeforeLongBreak = if (focusSessionsBeforeLongBreak >= NO_LONG_BREAK) NO_LONG_BREAK
        else focusSessionsBeforeLongBreak.coerceIn(MIN_CYCLES, MAX_CYCLES)
    )

    companion object {
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 180
        const val MIN_CYCLES = 2
        const val MAX_CYCLES = 12
        const val NO_LONG_BREAK = Int.MAX_VALUE

        fun of(mode: TimerMode): TimerPlan = TimerPlan(
            mode = mode,
            focusMinutes = mode.focusMinutes,
            shortBreakMinutes = mode.shortBreakMinutes,
            longBreakMinutes = mode.longBreakMinutes,
            focusSessionsBeforeLongBreak = mode.focusSessionsBeforeLongBreak
        )
    }
}
