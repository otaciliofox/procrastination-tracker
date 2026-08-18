package com.foxlab.procrastinationtracker.watch.service

import android.content.Context
import com.foxlab.procrastinationtracker.core.Session
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/**
 * Pushes a completed session to the paired phone via the Wearable Data Layer API.
 * The phone's WearSyncListenerService (app module) picks it up and stores it in
 * the shared history, so stats reflect time spent on either device.
 */
object WearSyncSender {

    fun sendSession(context: Context, session: Session) {
        val request = PutDataMapRequest.create(
            "/procrastination-tracker/session/${session.startTimeMillis}"
        ).apply {
            dataMap.putString("mode", session.mode.name)
            dataMap.putString("phase", session.phase.name)
            dataMap.putLong("startTimeMillis", session.startTimeMillis)
            dataMap.putLong("endTimeMillis", session.endTimeMillis)
            dataMap.putBoolean("completedFully", session.completedFully)
        }
        val putRequest = request.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context).putDataItem(putRequest)
    }
}
