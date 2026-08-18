package com.foxlab.procrastinationtracker.trackerdata

import android.content.Context
import android.provider.Settings
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.TimeUnit

/**
 * Publishes and identifies the live session. Lives in `:trackerdata` because both apps need the
 * exact same wire format and the same notion of "who am I" -- if the two sides disagreed on the
 * device id, every device would think the other one was itself and no hand-off would ever appear.
 */
object LiveSessionSync {

    /**
     * Stable per-install id. ANDROID_ID is per-device-per-app-signing-key, which is exactly the
     * granularity needed: the phone and the watch get different values even though the apps now
     * share a package name.
     */
    fun deviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
            .ifBlank { "unknown-device" }

    /**
     * Reads the other device's *current* broadcast instead of waiting for the next change event.
     *
     * Without this the hand-off prompt only appeared if the app happened to be open when a
     * heartbeat landed: a freshly started process has an empty in-memory cache, and the Data Layer
     * replays nothing on connect. Opening the app is exactly when the question matters, so the
     * app asks the Data Layer directly.
     */
    fun readRemote(context: Context): LiveSessionState? = runCatching {
        val items = Tasks.await(Wearable.getDataClient(context).dataItems, 5, TimeUnit.SECONDS)
        val mine = deviceId(context)
        val state = items
            .filter { it.uri.path == LIVE_SESSION_PATH }
            .map { LiveSessionCodec.decode(DataMapItem.fromDataItem(it).dataMap) }
            .firstOrNull { it.deviceId.isNotBlank() && it.deviceId != mine }
        items.release()
        state
    }.getOrNull()

    suspend fun publish(context: Context, state: LiveSessionState) {
        val request = PutDataMapRequest.create(LIVE_SESSION_PATH).apply {
            dataMap.putAll(LiveSessionCodec.encode(state))
        }
        // setUrgent: a hand-off offer is only useful while the other device is still counting.
        Wearable.getDataClient(context).putDataItem(request.asPutDataRequest().setUrgent())
    }

    /** Publishes "nobody is counting here", so the other device stops offering to take over. */
    suspend fun publishIdle(context: Context, deviceKind: String) {
        publish(
            context,
            LiveSessionState(
                deviceId = deviceId(context),
                deviceKind = deviceKind,
                isRunning = false,
                sessionId = "",
                sliceId = "",
                sliceTitle = "",
                startedAtMillis = 0L,
                heartbeatAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun publishRunning(
        context: Context,
        deviceKind: String,
        sessionId: String,
        sliceId: String,
        sliceTitle: String,
        startedAtMillis: Long
    ) {
        publish(
            context,
            LiveSessionState(
                deviceId = deviceId(context),
                deviceKind = deviceKind,
                isRunning = true,
                sessionId = sessionId,
                sliceId = sliceId,
                sliceTitle = sliceTitle,
                startedAtMillis = startedAtMillis,
                heartbeatAtMillis = System.currentTimeMillis()
            )
        )
    }
}
