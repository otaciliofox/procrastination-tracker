package com.foxlab.procrastinationtracker.core

import java.text.Normalizer

/**
 * Rules about *activities* that both apps and the data layer have to agree on. They lived in three
 * places before (the repository decided what counted as procrastination, the phone UI decided
 * colors, the phone ViewModel decided when a report was worth showing); centralising them here is
 * what keeps the watch from being a copy-paste of the phone.
 *
 * There is deliberately no cap on how many activities a profile may have on the watch: the board
 * is a stack of blocks that scrolls, so a ten-activity profile is a longer scroll, not a broken
 * screen.
 */
object ActivityRules {

    /** Below a minute tracked in total there is nothing worth plotting, so no report is offered. */
    const val MIN_REPORTABLE_MILLIS = 60_000L

    /**
     * Whether an activity counts as procrastination, by name and accent-insensitive. It stays a
     * name rule (not a flag) because the seeded Duo/Tri profiles and anything the user renames to
     * "procrastinando" should behave the same without a migration.
     */
    fun isProcrastination(title: String): Boolean =
        "procrastin" in Normalizer.normalize(title, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
}
