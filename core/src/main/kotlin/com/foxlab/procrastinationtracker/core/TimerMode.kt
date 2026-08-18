package com.foxlab.procrastinationtracker.core

/**
 * The two timing methods the original Procrastination Timer app offered.
 * Durations are in whole minutes for readability; convert with [minutesToMillis].
 */
enum class TimerMode(
    val label: String,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    /** How many focus sessions happen before a long break. Use Int.MAX_VALUE to disable. */
    val focusSessionsBeforeLongBreak: Int
) {
    /** 52 minutes focused, 17 minutes off. No long break variant. */
    FIFTY_TWO_SEVENTEEN(
        label = "52/17",
        focusMinutes = 52,
        shortBreakMinutes = 17,
        longBreakMinutes = 17,
        focusSessionsBeforeLongBreak = Int.MAX_VALUE
    ),

    /** Classic Pomodoro Technique: 25 min work, 5 min break, 30 min break every 4th cycle. */
    POMODORO(
        label = "Pomodoro",
        focusMinutes = 25,
        shortBreakMinutes = 5,
        longBreakMinutes = 30,
        focusSessionsBeforeLongBreak = 4
    ),

    /** 45 minutes focused, 15 minutes off. No long break variant. */
    FORTY_FIVE_FIFTEEN(
        label = "45/15",
        focusMinutes = 45,
        shortBreakMinutes = 15,
        longBreakMinutes = 15,
        focusSessionsBeforeLongBreak = Int.MAX_VALUE
    ),

    /**
     * User-defined durations. The values here are only the starting point shown the first time;
     * the real ones live in a [TimerPlan] the app persists and hands to the engine.
     */
    CUSTOM(
        label = "Custom",
        focusMinutes = 40,
        shortBreakMinutes = 10,
        longBreakMinutes = 20,
        focusSessionsBeforeLongBreak = 4
    );

    companion object {
        fun fromName(name: String): TimerMode =
            entries.firstOrNull { it.name == name } ?: FIFTY_TWO_SEVENTEEN
    }
}

fun Int.minutesToMillis(): Long = this * 60_000L
