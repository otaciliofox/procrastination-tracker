package com.foxlab.procrastinationtracker.core

/**
 * What Timer mode did in a day: finished cycles and real time spent. A countdown's history isn't
 * a weekly chart, it's "how many blocks did I close today", so this is the whole report.
 *
 * Lives in `:core` because it is arithmetic over [Session], not UI: the phone renders it as a
 * card and the watch can render the same numbers however a round screen needs to.
 */
data class TimerDaySummary(
    val completedFocusBlocks: Int = 0,
    val completedBreaks: Int = 0,
    val focusMillis: Long = 0L,
    val breakMillis: Long = 0L,
    /** Finished focus blocks per mode, e.g. "3 de 45/15". */
    val focusBlocksByMode: Map<TimerMode, Int> = emptyMap()
) {
    val hasAnything: Boolean get() = focusMillis > 0 || breakMillis > 0

    companion object {
        /**
         * Interrupted blocks count towards the time (they were still spent) but never towards the
         * cycle count -- that distinction is the reason this lives in one place instead of two.
         */
        fun from(sessions: List<Session>): TimerDaySummary {
            var focusMillis = 0L
            var breakMillis = 0L
            var completedFocus = 0
            var completedBreaks = 0
            val byMode = mutableMapOf<TimerMode, Int>()

            for (s in sessions) {
                if (s.phase == Phase.FOCUS) {
                    focusMillis += s.durationMillis
                    if (s.completedFully) {
                        completedFocus++
                        byMode[s.mode] = (byMode[s.mode] ?: 0) + 1
                    }
                } else {
                    breakMillis += s.durationMillis
                    if (s.completedFully) completedBreaks++
                }
            }

            return TimerDaySummary(
                completedFocusBlocks = completedFocus,
                completedBreaks = completedBreaks,
                focusMillis = focusMillis,
                breakMillis = breakMillis,
                focusBlocksByMode = byMode
            )
        }
    }
}
