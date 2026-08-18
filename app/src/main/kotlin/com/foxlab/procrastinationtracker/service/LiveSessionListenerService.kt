package com.foxlab.procrastinationtracker.service

import com.foxlab.procrastinationtracker.ProcrastinationTrackerApp
import com.foxlab.procrastinationtracker.trackerdata.CompanionPresence
import com.foxlab.procrastinationtracker.trackerdata.LIVE_SESSION_PATH
import com.foxlab.procrastinationtracker.trackerdata.LiveSessionCodec
import com.foxlab.procrastinationtracker.trackerdata.LiveSessionSync
import com.foxlab.procrastinationtracker.trackerdata.RemoteLiveSession
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Receives the watch's "I am counting X" broadcast.
 *
 * Two things happen here. The state is remembered so the UI can offer to take the block over, and
 * -- if the watch says it now owns a session this phone still has open -- the local row is dropped
 * without saving. That second part is what keeps a hand-off from counting the same stretch of time
 * twice, once on each device.
 */
class LiveSessionListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            if (event.dataItem.uri.path != LIVE_SESSION_PATH) return@forEach

            val state = LiveSessionCodec.decode(DataMapItem.fromDataItem(event.dataItem).dataMap)
            if (state.deviceId == LiveSessionSync.deviceId(this)) return@forEach

            // Any broadcast from the other side is the "hi": from now on this device knows a
            // companion exists and it is worth asking about hand-offs.
            CompanionPresence.markSeen(this, state.deviceKind)
            RemoteLiveSession.update(state)

            if (state.isRunning && state.sessionId.isNotBlank()) {
                // Route it through the service so the UI, the notification and the database all
                // stop together -- dropping only the row left this device showing a clock that
                // was no longer backed by anything.
                val release = Intent(this, TrackerForegroundService::class.java)
                    .setAction(TrackerForegroundService.ACTION_HANDOFF_RELEASE)
                    .putExtra(TrackerForegroundService.EXTRA_TAKEOVER_SESSION_ID, state.sessionId)
                ContextCompat.startForegroundService(this, release)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
