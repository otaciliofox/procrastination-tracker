package com.foxlab.procrastinationtracker.watch

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.foxlab.procrastinationtracker.trackerdata.TrackerDatabase
import com.foxlab.procrastinationtracker.trackerdata.LiveSessionSync
import com.foxlab.procrastinationtracker.trackerdata.SOURCE_WATCH
import com.foxlab.procrastinationtracker.trackerdata.TrackerRepository
import com.foxlab.procrastinationtracker.watch.service.ActivitySyncSender
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How much history the watch keeps locally. The phone stays the archive. */
private const val WATCH_HISTORY_WINDOW_MILLIS = 30L * 24 * 60 * 60 * 1000

@HiltAndroidApp
class WatchApplication : Application() {

    // Injected like everywhere else now; this class only needs it for the start-up work below.
    @Inject lateinit var trackerRepository: TrackerRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val TRACKER_NOTIFICATION_CHANNEL_ID = "tracker_channel"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TRACKER_NOTIFICATION_CHANNEL_ID, "Tracker de Atividades", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        appScope.launch {
            trackerRepository.ensureSeeded()
            trackerRepository.recoverStaleSessions()
            // The watch is a companion, not an archive: it keeps a rolling window and the phone
            // keeps the full history. This also keeps the sync payload inside the Data Layer's
            // size budget, which a growing snapshot would eventually blow past.
            trackerRepository.pruneSessionsOlderThan(WATCH_HISTORY_WINDOW_MILLIS)
            runCatching { ActivitySyncSender.push(this@WatchApplication, trackerRepository) }
            // "Oi" on every open: it tells the other device this one exists, which is what
            // decides whether hand-off questions are worth asking at all. It has to reflect
            // reality, though -- blindly publishing "idle" here would wipe this device's own
            // "I am counting" broadcast whenever the app process restarts mid-session.
            runCatching {
                val running = trackerRepository.getRunningSession()
                if (running == null) {
                    LiveSessionSync.publishIdle(this@WatchApplication, SOURCE_WATCH)
                } else {
                    val slice = trackerRepository.getSliceById(running.sliceId)
                    LiveSessionSync.publishRunning(
                        context = this@WatchApplication,
                        deviceKind = SOURCE_WATCH,
                        sessionId = running.id,
                        sliceId = running.sliceId,
                        sliceTitle = slice?.title.orEmpty(),
                        startedAtMillis = running.startTime
                    )
                }
            }
        }
        // Safety-net push every couple minutes; see the same comment in the phone's Application.
        appScope.launch {
            while (true) {
                delay(2 * 60 * 1000L)
                trackerRepository.pruneSessionsOlderThan(WATCH_HISTORY_WINDOW_MILLIS)
                runCatching { ActivitySyncSender.push(this@WatchApplication, trackerRepository) }
            }
        }
    }
}
