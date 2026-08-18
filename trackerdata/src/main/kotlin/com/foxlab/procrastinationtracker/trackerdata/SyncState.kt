package com.foxlab.procrastinationtracker.trackerdata

import android.content.Context

/**
 * When the last *full* reconciliation happened.
 *
 * Day to day the app only sends what is new, which keeps the payload well inside the Data Layer's
 * ~100KB ceiling. The cost of that is losing the self-healing property of a full snapshot: if a
 * delivery is dropped (Bluetooth out of range at the wrong moment, the other app reinstalled),
 * those rows are already marked as sent and would never be offered again. The periodic full pass
 * is the cheap insurance against exactly that.
 */
object SyncState {

    private const val PREFS = "tracker_sync_state"
    private const val KEY_LAST_FULL = "last_full_sync_at"

    /** How often the full 30-day snapshot is republished regardless of what changed. */
    const val FULL_SYNC_INTERVAL_MILLIS = 12L * 60 * 60 * 1000 // 12 hours

    fun shouldRunFullSync(context: Context): Boolean {
        val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_FULL, 0L)
        return System.currentTimeMillis() - last > FULL_SYNC_INTERVAL_MILLIS
    }

    fun markFullSyncDone(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_FULL, System.currentTimeMillis())
            .apply()
    }
}
