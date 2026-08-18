package com.foxlab.procrastinationtracker.trackerdata

import com.google.android.gms.wearable.DataMap
import java.util.concurrent.atomic.AtomicReference

/**
 * The *running* session, broadcast between devices.
 *
 * This is a different problem from syncing history: a finished block is a fact both devices can
 * merge, but a block that is still counting belongs to whoever is wearing/holding the device. So
 * instead of merging it, each device publishes "I am counting X since Y" and the other side can
 * offer to take it over -- see [TrackerSyncPayload] for the history half.
 */
data class LiveSessionState(
    /** Which device is counting. Empty means "nobody". */
    val deviceId: String,
    val deviceKind: String,
    val isRunning: Boolean,
    val sessionId: String,
    val sliceId: String,
    val sliceTitle: String,
    val startedAtMillis: Long,
    /** Refreshed with every heartbeat, so a stale broadcast can be ignored. */
    val heartbeatAtMillis: Long
) {
    /** A broadcast nobody refreshed for a while is a device that went away, not one still counting. */
    fun isFresh(nowMillis: Long = System.currentTimeMillis()): Boolean =
        isRunning && (nowMillis - heartbeatAtMillis) < LIVE_STALE_THRESHOLD_MILLIS

    fun elapsedMillis(nowMillis: Long = System.currentTimeMillis()): Long =
        (nowMillis - startedAtMillis).coerceAtLeast(0L)

    companion object {
        val IDLE = LiveSessionState("", "", false, "", "", "", 0L, 0L)
    }
}

/** Data Layer path for the live session. Separate from the history path so it can update often. */
const val LIVE_SESSION_PATH = "/procrastination-tracker/live-session"

/** Past this without a heartbeat, the other device is assumed gone (same budget as recovery). */
const val LIVE_STALE_THRESHOLD_MILLIS = 2 * 60 * 1000L

object LiveSessionCodec {

    fun encode(state: LiveSessionState): DataMap = DataMap().apply {
        putString("deviceId", state.deviceId)
        putString("deviceKind", state.deviceKind)
        putBoolean("isRunning", state.isRunning)
        putString("sessionId", state.sessionId)
        putString("sliceId", state.sliceId)
        putString("sliceTitle", state.sliceTitle)
        putLong("startedAtMillis", state.startedAtMillis)
        putLong("heartbeatAtMillis", state.heartbeatAtMillis)
    }

    fun decode(map: DataMap): LiveSessionState = LiveSessionState(
        deviceId = map.getString("deviceId").orEmpty(),
        deviceKind = map.getString("deviceKind").orEmpty(),
        isRunning = map.getBoolean("isRunning"),
        sessionId = map.getString("sessionId").orEmpty(),
        sliceId = map.getString("sliceId").orEmpty(),
        sliceTitle = map.getString("sliceTitle").orEmpty(),
        startedAtMillis = map.getLong("startedAtMillis"),
        heartbeatAtMillis = map.getLong("heartbeatAtMillis")
    )
}

/**
 * What the *other* device last told us it was doing. Held in memory on purpose: it is a hint used
 * while the app is open, and a stale hint that survived a reboot would be worse than no hint.
 */
object RemoteLiveSession {
    private val state = AtomicReference(LiveSessionState.IDLE)

    fun update(newState: LiveSessionState) = state.set(newState)

    fun current(): LiveSessionState = state.get()

    /** The other device is counting right now and this device isn't the one that published it. */
    fun runningOnOtherDevice(myDeviceId: String): LiveSessionState? =
        state.get().takeIf { it.isFresh() && it.deviceId.isNotBlank() && it.deviceId != myDeviceId }

    fun clear() = state.set(LiveSessionState.IDLE)
}
