package com.foxlab.procrastinationtracker.core

/**
 * A single completed (or in-progress) block of focus/break time.
 * This is the domain model shared between phone and watch; each platform
 * persists it however it likes (Room on the phone, a lightweight cache on the watch).
 */
data class Session(
    val id: Long = 0,
    val mode: TimerMode,
    val phase: Phase,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    /** false if the user stopped/skipped the session early. */
    val completedFully: Boolean,
    /** Which device produced this session: "phone" or "watch". Useful once synced. */
    val source: String = "phone"
) {
    val durationMillis: Long get() = endTimeMillis - startTimeMillis
}
