package com.foxlab.procrastinationtracker.core

/** What the current running (or last) block of time represents. */
enum class Phase {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK;

    val isBreak: Boolean get() = this == SHORT_BREAK || this == LONG_BREAK
}
