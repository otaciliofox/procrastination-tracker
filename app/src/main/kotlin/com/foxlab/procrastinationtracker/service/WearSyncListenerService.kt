package com.foxlab.procrastinationtracker.service

import com.foxlab.procrastinationtracker.ProcrastinationTrackerApp
import com.foxlab.procrastinationtracker.core.Phase
import com.foxlab.procrastinationtracker.core.Session
import com.foxlab.procrastinationtracker.core.TimerMode
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives completed sessions pushed from the Galaxy Watch app (see
 * `wear` module's WearSyncSender) via the Wearable Data Layer API and
 * stores them in the same Room database used by the phone's own timer,
 * so History/Stats show a combined picture.
 */
class WearSyncListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob())

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val repository = (application as ProcrastinationTrackerApp).repository
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path ?: continue
            if (!path.startsWith("/procrastination-tracker/session")) continue

            val map = DataMapItem.fromDataItem(event.dataItem).dataMap
            val session = Session(
                mode = TimerMode.fromName(map.getString("mode", TimerMode.FIFTY_TWO_SEVENTEEN.name)),
                phase = Phase.valueOf(map.getString("phase", Phase.FOCUS.name)),
                startTimeMillis = map.getLong("startTimeMillis"),
                endTimeMillis = map.getLong("endTimeMillis"),
                completedFully = map.getBoolean("completedFully", true),
                source = "watch"
            )
            scope.launch { repository.record(session) }
        }
    }
}
