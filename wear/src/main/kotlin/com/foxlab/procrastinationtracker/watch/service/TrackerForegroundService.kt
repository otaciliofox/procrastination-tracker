package com.foxlab.procrastinationtracker.watch.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.foxlab.procrastinationtracker.core.toClockString
import com.foxlab.procrastinationtracker.trackerdata.LiveSessionSync
import com.foxlab.procrastinationtracker.trackerdata.TrackerRepository
import com.foxlab.procrastinationtracker.watch.R
import com.foxlab.procrastinationtracker.watch.WatchApplication
import com.foxlab.procrastinationtracker.watch.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Watch-side twin of the phone's TrackerForegroundService. Same behavior, see spec 002 §4.1/§6. */
class TrackerForegroundService : Service() {

    data class UiState(
        val activeSliceId: String? = null,
        val activeSliceTitle: String? = null,
        val elapsedMillis: Long = 0L
    ) {
        val isTracking: Boolean get() = activeSliceId != null
    }

    companion object {
        const val ACTION_ACTIVATE_SLICE = "com.foxlab.procrastinationtracker.watch.tracker.action.ACTIVATE_SLICE"
        const val ACTION_PAUSE = "com.foxlab.procrastinationtracker.watch.tracker.action.PAUSE"
        const val ACTION_STOP = "com.foxlab.procrastinationtracker.watch.tracker.action.STOP"
        const val ACTION_DISCARD = "com.foxlab.procrastinationtracker.watch.tracker.action.DISCARD"
        const val ACTION_SWITCH_PROFILE = "com.foxlab.procrastinationtracker.watch.tracker.action.SWITCH_PROFILE"

        /** Notification-only: resume what was running before the pause. */
        const val ACTION_RESUME = "com.foxlab.procrastinationtracker.watch.tracker.action.RESUME"

        /** Notification-only: jump to the next activity of the profile. */
        const val ACTION_NEXT_SLICE = "com.foxlab.procrastinationtracker.watch.tracker.action.NEXT_SLICE"

        /** Continues, on this device, the block the other one was counting. */
        const val ACTION_TAKE_OVER = "com.foxlab.procrastinationtracker.watch.tracker.action.TAKE_OVER"
        const val EXTRA_TAKEOVER_SESSION_ID = "extra_takeover_session_id"
        const val EXTRA_TAKEOVER_STARTED_AT = "extra_takeover_started_at"

        /** The other device took the block over: let go of it here, quietly. */
        const val ACTION_HANDOFF_RELEASE = "com.foxlab.procrastinationtracker.watch.tracker.action.HANDOFF_RELEASE"
        const val EXTRA_SLICE_ID = "extra_slice_id"
        const val EXTRA_SLICE_TITLE = "extra_slice_title"
        const val EXTRA_PROFILE_ID = "extra_profile_id"
        /** Tints the glyph with the app's blue, same as the phone. */
        private const val BRAND_COLOR = 0xFF3B82F6.toInt()
        private const val NOTIFICATION_ID = 44
        private const val HEARTBEAT_EVERY_TICKS = 30

        private val _uiState = MutableStateFlow(UiState())
        val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    }

    private var tickingJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob())
    private lateinit var repository: TrackerRepository
    private var startedAtElapsedRealtime: Long = 0L

    /** Wall-clock start of the running block, so the notification counts on its own. */
    private var startedAtWallClock: Long = 0L
    private var lastSliceId: String? = null
    private var lastSliceTitle: String = ""

    override fun onCreate() {
        super.onCreate()
        repository = (application as WatchApplication).trackerRepository
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ACTIVATE_SLICE -> {
                val sliceId = intent.getStringExtra(EXTRA_SLICE_ID) ?: return START_STICKY
                activate(sliceId, intent.getStringExtra(EXTRA_SLICE_TITLE).orEmpty())
            }
            ACTION_RESUME -> {
                val sliceId = lastSliceId
                if (sliceId == null) {
                    scope.launch {
                        val slice = repository.getNextSlice(null)
                        if (slice != null) withContext(Dispatchers.Main) { activate(slice.id, slice.title) }
                    }
                } else {
                    activate(sliceId, lastSliceTitle)
                }
            }
            ACTION_HANDOFF_RELEASE -> {
                // The row is dropped (not saved) and this device stops counting -- but it must
                // NOT broadcast "idle", or it would overwrite the other device's fresh "I am
                // counting this now" and each side would think nobody owns the block.
                // Close and *save* the time this device measured, then stop. Saving (instead of
                // dropping the row) is what makes a hand-off survive the other device dying right
                // after taking over -- the minutes counted here are already on disk.
                scope.launch {
                    repository.handOverRunningSession()
                    syncNow()
                }
                tickingJob?.cancel()
                _uiState.value = UiState()
                updateNotification()
            }
            ACTION_TAKE_OVER -> {
                val sessionId = intent.getStringExtra(EXTRA_TAKEOVER_SESSION_ID)
                val sliceId = intent.getStringExtra(EXTRA_SLICE_ID)
                val title = intent.getStringExtra(EXTRA_SLICE_TITLE).orEmpty()
                val startedAt = intent.getLongExtra(EXTRA_TAKEOVER_STARTED_AT, System.currentTimeMillis())
                if (sessionId != null && sliceId != null) takeOver(sessionId, sliceId, title, startedAt)
            }
            ACTION_NEXT_SLICE -> {
                val current = _uiState.value.activeSliceId ?: lastSliceId
                scope.launch {
                    val next = repository.getNextSlice(current)
                    if (next != null) withContext(Dispatchers.Main) { activate(next.id, next.title) }
                }
            }
            ACTION_PAUSE -> {
                scope.launch { repository.pauseActive(); syncNow() }
                goIdle()
            }
            ACTION_DISCARD -> {
                scope.launch { repository.discardActiveSession() }
                goIdle()
            }
            ACTION_SWITCH_PROFILE -> {
                val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
                if (profileId != null) scope.launch { repository.switchActiveProfile(profileId); syncNow() }
                goIdle()
            }
            ACTION_STOP -> {
                scope.launch { repository.stopActive(); syncNow() }
                goIdle()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun activate(sliceId: String, title: String) {
        val startedAt = System.currentTimeMillis()
        scope.launch {
            val sessionId = repository.activateSlice(sliceId, "watch")
            publishLive(sessionId, sliceId, title, startedAt)
            syncNow()
        }
        startedAtElapsedRealtime = SystemClock.elapsedRealtime()
        startedAtWallClock = startedAt
        lastSliceId = sliceId
        lastSliceTitle = title
        _uiState.value = UiState(activeSliceId = sliceId, activeSliceTitle = title, elapsedMillis = 0L)
        startTicking()
        updateNotification()
    }

    /**
     * Continues a block started on the other device: same session id, same start time, new owner.
     * The other side drops its open row when it sees this broadcast, so the stretch of time is
     * counted once even though two devices touched it.
     */
    private fun takeOver(sessionId: String, sliceId: String, title: String, startedAtMillis: Long) {
        val now = System.currentTimeMillis()
        val carried = (now - startedAtMillis).coerceAtLeast(0L)
        scope.launch {
            val newId = repository.startContinuationSession(
                sliceId = sliceId,
                sourceDevice = "watch",
                carriedMillis = carried,
                continuedFromSessionId = sessionId
            )
            // Broadcast the *continuous* start so the other device shows the number the user
            // experienced, while each row keeps the time its own device really measured.
            publishLive(newId, sliceId, title, startedAtMillis)
            syncNow()
        }
        startedAtElapsedRealtime = SystemClock.elapsedRealtime()
        startedAtWallClock = now
        lastSliceId = sliceId
        lastSliceTitle = title
        _uiState.value = UiState(activeSliceId = sliceId, activeSliceTitle = title, elapsedMillis = 0L)
        startTicking()
        updateNotification()
    }

    private suspend fun publishLive(sessionId: String, sliceId: String, title: String, startedAtMillis: Long) {
        runCatching {
            LiveSessionSync.publishRunning(
                context = applicationContext,
                deviceKind = "watch",
                sessionId = sessionId,
                sliceId = sliceId,
                sliceTitle = title,
                startedAtMillis = startedAtMillis
            )
        }
    }

    private suspend fun publishIdle() {
        runCatching { LiveSessionSync.publishIdle(applicationContext, "watch") }
    }

    private fun goIdle() {
        tickingJob?.cancel()
        _uiState.value = UiState()
        scope.launch { publishIdle() }
        updateNotification()
    }

    private suspend fun syncNow() {
        runCatching { ActivitySyncSender.push(applicationContext, repository) }
    }

    private fun startTicking() {
        tickingJob?.cancel()
        tickingJob = scope.launch {
            var ticks = 0
            while (true) {
                delay(1000)
                val elapsed = SystemClock.elapsedRealtime() - startedAtElapsedRealtime
                _uiState.value = _uiState.value.copy(elapsedMillis = elapsed)
                ticks++
                if (ticks % HEARTBEAT_EVERY_TICKS == 0) {
                    repository.heartbeatActive()
                    // Refreshing the broadcast is what lets the other device tell "still
                    // counting" apart from "that device went away mid-session".
                    val state = _uiState.value
                    val runningId = repository.getRunningSession()?.id
                    val activeSlice = state.activeSliceId
                    if (runningId != null && activeSlice != null) {
                        publishLive(runningId, activeSlice, state.activeSliceTitle.orEmpty(), startedAtWallClock)
                    }
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        scope.launch { repository.stopActive() }
        goIdle()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    private fun serviceAction(requestCode: Int, action: String): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, TrackerForegroundService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /**
     * Same remote control as the phone, sized for the wrist: activity name, a clock the system
     * counts by itself, and pause/resume, switch and stop right there. On a watch this matters
     * even more -- the notification is often closer than reopening the app.
     */
    private fun buildNotification(): Notification {
        val state = _uiState.value
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, WatchApplication.TRACKER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BRAND_COLOR)
            .setContentIntent(openAppIntent)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)

        if (state.isTracking) {
            builder
                .setContentTitle(state.activeSliceTitle)
                .setContentText("Rastreando agora")
                .setUsesChronometer(true)
                .setWhen(startedAtWallClock)
                .setShowWhen(true)
                .setOngoing(true)
                .addAction(0, "Pausar", serviceAction(2, ACTION_PAUSE))
                .addAction(0, "Trocar", serviceAction(3, ACTION_NEXT_SLICE))
                .addAction(0, "Parar", serviceAction(1, ACTION_STOP))
        } else {
            builder
                .setContentTitle("Tracker pausado")
                .setContentText(
                    lastSliceTitle.takeIf { it.isNotBlank() }
                        ?.let { "Última atividade: $it" }
                        ?: "Toque numa atividade para retomar"
                )
                .setUsesChronometer(false)
                .setShowWhen(false)
                .setOngoing(false)
                .addAction(0, "Retomar", serviceAction(4, ACTION_RESUME))
                .addAction(0, "Parar", serviceAction(1, ACTION_STOP))
        }
        return builder.build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
