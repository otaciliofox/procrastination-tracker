package com.foxlab.procrastinationtracker.service

import com.foxlab.procrastinationtracker.ProcrastinationTrackerApp
import com.foxlab.procrastinationtracker.trackerdata.ACTIVITY_SYNC_PATH
import com.foxlab.procrastinationtracker.trackerdata.TrackerSyncCodec
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Receives a Tracker-mode snapshot pushed from the watch and merges it in. Spec 002 §7. */
class ActivitySyncListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob())

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val repository = (application as ProcrastinationTrackerApp).trackerRepository
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            if (event.dataItem.uri.path != ACTIVITY_SYNC_PATH) continue
            val payload = TrackerSyncCodec.decode(DataMapItem.fromDataItem(event.dataItem).dataMap)
            scope.launch { repository.mergeSyncPayload(payload) }
        }
    }
}
