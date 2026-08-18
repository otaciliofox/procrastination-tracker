package com.foxlab.procrastinationtracker.trackerdata

import android.content.Context

/**
 * Remembers whether this device has ever heard from a companion (watch or phone).
 *
 * Every app open publishes a "hi", and receiving one is what flips this flag. Without it, a
 * user who owns no watch would still pay for the hand-off machinery on every screen open --
 * the Data Layer lookup blocks for up to five seconds when there is nobody to answer. If no
 * "hi" ever arrived, there is nothing to ask about, so the app just starts counting.
 *
 * The flag is sticky on purpose: a watch that is paired but sitting in a drawer today is still
 * a watch the user has, and the freshness check on the live broadcast already handles "it's
 * there but not in use".
 */
object CompanionPresence {

    private const val PREFS = "companion_presence"
    private const val KEY_SEEN = "has_seen_companion"
    private const val KEY_KIND = "companion_kind"
    private const val KEY_LAST_SEEN = "last_seen_at"

    fun markSeen(context: Context, deviceKind: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SEEN, true)
            .putString(KEY_KIND, deviceKind)
            .putLong(KEY_LAST_SEEN, System.currentTimeMillis())
            .apply()
    }

    fun hasCompanion(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SEEN, false)

    /** "watch" or "phone" -- what the other side calls itself, for wording the question. */
    fun companionKind(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_KIND, "").orEmpty()

    fun lastSeenAt(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_SEEN, 0L)
}
