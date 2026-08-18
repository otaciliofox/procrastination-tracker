package com.foxlab.procrastinationtracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.foxlab.procrastinationtracker.data.AppDatabase
import com.foxlab.procrastinationtracker.data.SessionRepository
import com.foxlab.procrastinationtracker.service.ActivitySyncSender
import com.foxlab.procrastinationtracker.trackerdata.TrackerDatabase
import com.foxlab.procrastinationtracker.trackerdata.LiveSessionSync
import com.foxlab.procrastinationtracker.trackerdata.SOURCE_PHONE
import com.foxlab.procrastinationtracker.trackerdata.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProcrastinationTrackerApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: SessionRepository by lazy { SessionRepository(database.sessionDao()) }

    val trackerDatabase: TrackerDatabase by lazy { TrackerDatabase.build(this) }
    val trackerRepository: TrackerRepository by lazy { TrackerRepository(trackerDatabase) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val TIMER_NOTIFICATION_CHANNEL_ID = "timer_channel"
        const val TIMER_ALERT_CHANNEL_ID = "timer_alert_channel"
        const val TRACKER_NOTIFICATION_CHANNEL_ID = "tracker_channel"
        const val BUBBLE_CHANNEL_ID = "tracker_bubble_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        appScope.launch {
            trackerRepository.ensureSeeded()
            trackerRepository.recoverStaleSessions()
            runCatching { ActivitySyncSender.push(this@ProcrastinationTrackerApp, trackerRepository) }
            // "Oi" on every open: it tells the other device this one exists, which is what
            // decides whether hand-off questions are worth asking at all. It has to reflect
            // reality, though -- blindly publishing "idle" here would wipe this device's own
            // "I am counting" broadcast whenever the app process restarts mid-session.
            runCatching {
                val running = trackerRepository.getRunningSession()
                if (running == null) {
                    LiveSessionSync.publishIdle(this@ProcrastinationTrackerApp, SOURCE_PHONE)
                } else {
                    val slice = trackerRepository.getSliceById(running.sliceId)
                    LiveSessionSync.publishRunning(
                        context = this@ProcrastinationTrackerApp,
                        deviceKind = SOURCE_PHONE,
                        sessionId = running.id,
                        sliceId = running.sliceId,
                        sliceTitle = slice?.title.orEmpty(),
                        startedAtMillis = running.startTime
                    )
                }
            }
        }
        // Safety-net push every couple minutes, on top of the explicit pushes TrackerForegroundService
        // fires right after a session closes. Keeps the watch caught up even if a push failed or a
        // write happened while it was out of Bluetooth range. See spec 002 §7.
        appScope.launch {
            while (true) {
                delay(2 * 60 * 1000L)
                runCatching { ActivitySyncSender.push(this@ProcrastinationTrackerApp, trackerRepository) }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                TIMER_NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.notification_channel_description) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                TRACKER_NOTIFICATION_CHANNEL_ID,
                getString(R.string.tracker_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.tracker_notification_channel_description) }
        )
        // The bubble lives in its own channel so the user can turn the floating control off
        // without losing the ongoing notification (they are different features to them).
        manager.createNotificationChannel(
            NotificationChannel(
                BUBBLE_CHANNEL_ID,
                getString(R.string.bubble_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.bubble_channel_description)
                setAllowBubbles(true)
            }
        )
        // The countdown notification is silent on purpose; the end-of-block alert is the one
        // allowed to make noise, so it needs a channel of its own.
        manager.createNotificationChannel(
            NotificationChannel(
                TIMER_ALERT_CHANNEL_ID,
                getString(R.string.timer_alert_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = getString(R.string.timer_alert_channel_description) }
        )
    }
}
