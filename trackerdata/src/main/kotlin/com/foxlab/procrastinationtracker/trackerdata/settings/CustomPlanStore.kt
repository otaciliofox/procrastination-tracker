package com.foxlab.procrastinationtracker.trackerdata.settings

import android.content.Context
import com.foxlab.procrastinationtracker.core.TimerMode
import com.foxlab.procrastinationtracker.core.TimerPlan

/**
 * Where the user's own durations live. It sits in `:trackerdata` -- the Android layer both apps
 * already share -- rather than in the phone module, so the watch can read the same plan when it
 * gains the Custom mode, and in `:core` it couldn't live at all (that module is pure Kotlin, no
 * Android types). Small enough that SharedPreferences is the right tool: four integers, read when
 * the timer starts and when the editor opens.
 */
object CustomPlanStore {

    private const val PREFS = "timer_custom_plan"
    private const val KEY_FOCUS = "focus"
    private const val KEY_SHORT = "short_break"
    private const val KEY_LONG = "long_break"
    private const val KEY_CYCLES = "cycles"

    fun load(context: Context): TimerPlan {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val default = TimerPlan.of(TimerMode.CUSTOM)
        return TimerPlan(
            mode = TimerMode.CUSTOM,
            focusMinutes = prefs.getInt(KEY_FOCUS, default.focusMinutes),
            shortBreakMinutes = prefs.getInt(KEY_SHORT, default.shortBreakMinutes),
            longBreakMinutes = prefs.getInt(KEY_LONG, default.longBreakMinutes),
            focusSessionsBeforeLongBreak = prefs.getInt(KEY_CYCLES, default.focusSessionsBeforeLongBreak)
        ).sanitized()
    }

    fun save(context: Context, plan: TimerPlan) {
        val clean = plan.copy(mode = TimerMode.CUSTOM).sanitized()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_FOCUS, clean.focusMinutes)
            .putInt(KEY_SHORT, clean.shortBreakMinutes)
            .putInt(KEY_LONG, clean.longBreakMinutes)
            .putInt(KEY_CYCLES, clean.focusSessionsBeforeLongBreak)
            .apply()
    }

    /** The plan for any mode: presets come from the enum, CUSTOM comes from disk. */
    fun planFor(context: Context, mode: TimerMode): TimerPlan =
        if (mode == TimerMode.CUSTOM) load(context) else TimerPlan.of(mode)
}
