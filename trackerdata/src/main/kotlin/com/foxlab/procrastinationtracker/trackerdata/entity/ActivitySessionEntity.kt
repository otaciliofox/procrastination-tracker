package com.foxlab.procrastinationtracker.trackerdata.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_session")
data class ActivitySessionEntity(
    @PrimaryKey val id: String,
    val sliceId: String,
    /** NULL = time not tied to a specific subtask. Reserved for Fase 2; unused in Fase 1. */
    val subtaskId: String?,
    val startTime: Long,
    /** NULL while the session is running. */
    val endTime: Long?,
    /**
     * Updated every ~30s while running (see TrackerForegroundService). Lets the app recover
     * cleanly if the process dies without a clean stop: on next launch, any session still open
     * with a stale heartbeat gets closed using this timestamp instead of running forever.
     */
    val lastHeartbeatAt: Long?,
    val sourceDevice: String,
    /**
     * Set when this block continues one that was running on the other device. The two rows stay
     * separate on purpose: each device keeps the time it actually measured (so the reports can
     * say "this much came from the watch"), while the app shows the user the single continuous
     * number they experienced.
     */
    val continuedFromSessionId: String? = null,
    /**
     * How much time was already banked on the other device when this block took over. Only used
     * to display the running total; it is *not* part of this row's duration, otherwise the same
     * minutes would be counted twice.
     *
     * Stored rather than derived because the other device's row may not have synced yet (or ever,
     * if the phone is left behind) -- and the whole point of this design is that closing the app
     * mid-block can't lose time that already happened.
     */
    val carriedMillis: Long = 0L,
    /**
     * When this row was last handed to the other device. Null means "never sent".
     *
     * Sync used to publish a full 30-day snapshot every time, which is self-healing but grows:
     * a Data Layer item is capped at ~100KB, so with real usage the snapshot would eventually
     * stop fitting and sync would silently die. Sending only what is new keeps it small; a
     * periodic full pass (see SyncState) is what recovers anything a lost delivery skipped.
     */
    val syncedAt: Long? = null,
    val createdAt: Long
)
