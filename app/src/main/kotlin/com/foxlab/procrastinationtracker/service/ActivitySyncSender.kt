package com.foxlab.procrastinationtracker.service

import android.content.Context
import com.foxlab.procrastinationtracker.trackerdata.ACTIVITY_SYNC_PATH
import com.foxlab.procrastinationtracker.trackerdata.SyncState
import com.foxlab.procrastinationtracker.trackerdata.TrackerRepository
import com.foxlab.procrastinationtracker.trackerdata.TrackerSyncCodec
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.TimeUnit

/**
 * Publishes Tracker-mode history to the other device (spec 002 §7).
 *
 * Incremental by default -- only blocks the other side has never received -- with a full
 * republish every [SyncState.FULL_SYNC_INTERVAL_MILLIS]. Rows are marked as sent only after the
 * write actually completes, so a failed publish is retried by the next push instead of being
 * silently forgotten.
 */
object ActivitySyncSender {

    suspend fun push(context: Context, repository: TrackerRepository) {
        val full = SyncState.shouldRunFullSync(context)
        val snapshot = repository.snapshotForSync(forceFull = full)

        // Nothing new and no reconciliation due: publishing an identical item would be a no-op
        // for the Data Layer anyway.
        if (!full && snapshot.sessionIds.isEmpty() && snapshot.payload.deletedSessionIds.isEmpty()) return

        val request = PutDataMapRequest.create(ACTIVITY_SYNC_PATH).apply {
            dataMap.putAll(TrackerSyncCodec.encode(snapshot.payload))
        }
        Tasks.await(
            Wearable.getDataClient(context).putDataItem(request.asPutDataRequest().setUrgent()),
            15,
            TimeUnit.SECONDS
        )

        repository.markSessionsSynced(snapshot.sessionIds)
        if (full) SyncState.markFullSyncDone(context)
    }
}
