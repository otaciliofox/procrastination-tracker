package com.foxlab.procrastinationtracker.core.theme

import com.foxlab.procrastinationtracker.core.ActivityRules

/**
 * Design tokens for the board visual language, as plain ARGB longs so the phone (Compose) and the
 * watch (Compose for Wear OS) can each wrap them in their own `Color(...)` without the values ever
 * diverging. This is the shared half of the design system; anything that needs an Android type
 * (drawables, XML themes, splash background) is platform-specific and lives in each app module.
 */
object BoardTokens {

    // --- Canvas: everything is drawn on this near-black navy, system bars included.
    const val CANVAS: Long = 0xFF0A1020
    const val CANVAS_TOP: Long = 0xFF101A33
    const val SURFACE: Long = 0xFF131F3A
    const val SURFACE_VARIANT: Long = 0xFF1A2848

    // --- Ink
    const val INK: Long = 0xFFE9EFFB
    const val INK_MUTED: Long = 0xFF98A7C4
    const val OUTLINE: Long = 0xFF2B3B5E
    const val OUTLINE_SOFT: Long = 0xFF1F2C4A

    // --- Accents. BLUE is the app's primary; ROSE is reserved for procrastination and for the
    // break phase, so "time that isn't focus" always reads the same way.
    const val BLUE: Long = 0xFF3B82F6
    const val TEAL: Long = 0xFF14B8A6
    const val VIOLET: Long = 0xFF8B5CF6
    const val AMBER: Long = 0xFFF59E0B
    const val CYAN: Long = 0xFF22D3EE
    const val LIME: Long = 0xFF84CC16
    const val PINK: Long = 0xFFEC4899
    const val ORANGE: Long = 0xFFF97316
    const val INDIGO: Long = 0xFF6366F1
    const val SLATE: Long = 0xFF94A3B8
    const val ROSE: Long = 0xFFF43F5E

    const val GO_GREEN: Long = 0xFF22C55E

    /** One color per activity, by position, for up to the 10 a profile can hold. */
    val ACTIVITY_COLORS: List<Long> = listOf(BLUE, TEAL, VIOLET, AMBER, CYAN, LIME, PINK, ORANGE, INDIGO, SLATE)

    /**
     * The color an activity carries everywhere -- board, report, watch. Procrastination always
     * takes [ROSE] wherever it sits in the list; the rest follow their position.
     */
    fun activityColor(title: String, index: Int): Long =
        if (ActivityRules.isProcrastination(title)) ROSE
        else ACTIVITY_COLORS[((index % ACTIVITY_COLORS.size) + ACTIVITY_COLORS.size) % ACTIVITY_COLORS.size]
}
