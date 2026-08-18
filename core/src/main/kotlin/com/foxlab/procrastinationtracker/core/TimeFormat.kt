package com.foxlab.procrastinationtracker.core

/** mm:ss for a running countdown, e.g. 52:00, 04:59. */
fun Long.toClockString(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

/**
 * Counting *up*: "4:07" under an hour, "1:04:07" past it. Used by the tracker board on both the
 * phone and the watch, so an activity's time is never formatted two different ways.
 */
fun Long.toStopwatchString(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

/** Human-readable "1h 23m" style, used on stats screens. */
fun Long.toHoursMinutesString(): String {
    val totalMinutes = (this / 60_000).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
