package com.foxlab.procrastinationtracker.service

import com.foxlab.procrastinationtracker.trackerdata.ACTIVITY_SYNC_PATH
import com.foxlab.procrastinationtracker.trackerdata.TrackerRepository
import com.foxlab.procrastinationtracker.trackerdata.TrackerSyncCodec
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Receives a Tracker-mode snapshot pushed from the watch and merges it in. Spec 002 §7. */
@AndroidEntryPoint
class ActivitySyncListenerService : WearableListenerService() {

    @Inject lateinit var repository: TrackerRepository

    private val scope = CoroutineScope(SupervisorJob())

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            if (event.dataItem.uri.path != ACTIVITY_SYNC_PATH) continue
            val payload = TrackerSyncCodec.decode(DataMapItem.fromDataItem(event.dataItem).dataMap)
            scope.launch { repository.mergeSyncPayload(payload) }
        }
    }
}
