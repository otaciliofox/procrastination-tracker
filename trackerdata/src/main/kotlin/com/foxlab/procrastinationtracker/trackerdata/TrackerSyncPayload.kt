package com.foxlab.procrastinationtracker.trackerdata

import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySessionEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.LayoutProfileEntity

/** Everything sent in one sync round-trip. See spec 002 §7. */
data class TrackerSyncPayload(
    val profiles: List<LayoutProfileEntity>,
    val slices: List<ActivitySliceEntity>,
    val sessions: List<ActivitySessionEntity>,
    /**
     * Sessions deleted on purpose (day reset, manual correction). Sync is otherwise additive, so
     * without these the other device would push deleted rows back and undo the correction.
     */
    val deletedSessionIds: List<String> = emptyList()
)

/** Path used on the Wearable Data Layer for Tracker-mode sync (distinct from Spec 001's Timer path). */
const val ACTIVITY_SYNC_PATH = "/procrastination-tracker/activity-sync"

/**
 * How far back to sync sessions. Matches the 30-day window the watch keeps locally: sending more
 * than the other side is willing to store is wasted bytes, and a Data Layer item is capped at
 * ~100KB, so the payload has to stay bounded. The phone remains the full archive.
 */
const val SYNC_SESSION_WINDOW_MILLIS = 30L * 24 * 60 * 60 * 1000 // 30 days
