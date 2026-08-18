package com.foxlab.procrastinationtracker.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.foxlab.procrastinationtracker.MainActivity
import com.foxlab.procrastinationtracker.ProcrastinationTrackerApp
import com.foxlab.procrastinationtracker.R
import com.foxlab.procrastinationtracker.core.toClockString
import com.foxlab.procrastinationtracker.trackerdata.LiveSessionSync
import com.foxlab.procrastinationtracker.trackerdata.TrackerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the single "what slice is active right now" state for the phone's Tracker mode. Mirrors
 * TimerForegroundService's architecture: UI never binds to this service, it just fires action
 * intents and observes [uiState]. See spec 002 §4.1 and §6.
 */
@AndroidEntryPoint
class TrackerForegroundService : Service() {

    data class UiState(
        val activeSliceId: String? = null,
        val activeSliceTitle: String? = null,
        val elapsedMillis: Long = 0L
    ) {
        val isTracking: Boolean get() = activeSliceId != null
    }

    companion object {
        const val ACTION_ACTIVATE_SLICE = "com.foxlab.procrastinationtracker.tracker.action.ACTIVATE_SLICE"
        const val ACTION_PAUSE = "com.foxlab.procrastinationtracker.tracker.action.PAUSE"
        const val ACTION_STOP = "com.foxlab.procrastinationtracker.tracker.action.STOP"
        const val ACTION_DISCARD = "com.foxlab.procrastinationtracker.tracker.action.DISCARD"
        const val ACTION_SWITCH_PROFILE = "com.foxlab.procrastinationtracker.tracker.action.SWITCH_PROFILE"

        /** Notification-only: resume the activity that was running before the pause. */
        const val ACTION_RESUME = "com.foxlab.procrastinationtracker.tracker.action.RESUME"

        /** Notification-only: jump to the next activity of the profile (Duo = toggle the two). */
        const val ACTION_NEXT_SLICE = "com.foxlab.procrastinationtracker.tracker.action.NEXT_SLICE"

        /** Continues, on this device, the block the other one was counting. */
        const val ACTION_TAKE_OVER = "com.foxlab.procrastinationtracker.tracker.action.TAKE_OVER"
        const val EXTRA_TAKEOVER_SESSION_ID = "extra_takeover_session_id"
        const val EXTRA_TAKEOVER_STARTED_AT = "extra_takeover_started_at"

        /** The other device took the block over: let go of it here, quietly. */
        const val ACTION_HANDOFF_RELEASE = "com.foxlab.procrastinationtracker.tracker.action.HANDOFF_RELEASE"
        const val EXTRA_SLICE_ID = "extra_slice_id"
        const val EXTRA_SLICE_TITLE = "extra_slice_title"
        const val EXTRA_PROFILE_ID = "extra_profile_id"
        /** Tints the glyph in the shade with the app's blue. */
        private const val BRAND_COLOR = 0xFF3B82F6.toInt()
        private const val NOTIFICATION_ID = 43
        private const val HEARTBEAT_EVERY_TICKS = 30 // 30 ticks of 1s = 30s, per spec §2/§6

        private val _uiState = MutableStateFlow(UiState())
        val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    }

    private var tickingJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob())
    @Inject lateinit var repository: TrackerRepository
    private var startedAtElapsedRealtime: Long = 0L

    /** Wall-clock start of the running block, so the notification's chronometer counts by itself. */
    private var startedAtWallClock: Long = 0L

    /** What "Retomar" goes back to after a pause. */
    private var lastSliceId: String? = null
    private var lastSliceTitle: String = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ACTIVATE_SLICE -> {
                val sliceId = intent.getStringExtra(EXTRA_SLICE_ID) ?: return START_STICKY
                activate(sliceId, intent.getStringExtra(EXTRA_SLICE_TITLE).orEmpty())
            }
            ACTION_RESUME -> {
                val sliceId = lastSliceId
                if (sliceId == null) {
                    // Nothing to go back to (fresh service): fall back to the profile's first activity.
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
            val sessionId = repository.activateSlice(sliceId, "phone")
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
                sourceDevice = "phone",
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
                deviceKind = "phone",
                sessionId = sessionId,
                sliceId = sliceId,
                sliceTitle = title,
                startedAtMillis = startedAtMillis
            )
        }
    }

    private suspend fun publishIdle() {
        runCatching { LiveSessionSync.publishIdle(applicationContext, "phone") }
    }

    private fun goIdle() {
        tickingJob?.cancel()
        _uiState.value = UiState()
        scope.launch { publishIdle() }
        updateNotification()
    }

    /** Best-effort push right after a session closes; the 2-minute app-level loop is the safety net. */
    private suspend fun syncNow() {
        runCatching { ActivitySyncSender.push(applicationContext, repository) }
    }

    /**
     * Ticks the in-app clock every second, but no longer re-posts the notification each time --
     * the notification counts on its own via `setUsesChronometer`, so it is only rebuilt when the
     * state actually changes (start/pause/switch). One notification per second was a real battery
     * cost for a number the system can render itself.
     */
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

    /**
     * Swiping the app away from Recents fires this — treat exactly like "Parar": close and save
     * whatever was running, then let the service die instead of the default sticky restart.
     * See spec 002 §6.
     */
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
     * The notification *is* the remote control: activity name, a clock the system keeps counting
     * on its own, and the same three moves the board offers -- pause/resume, switch activity, stop
     * -- so leaving the app for Duolingo or Instagram never means losing control of the tracking.
     * Tapping anywhere on it opens the app.
     */
    private fun buildNotification(): Notification {
        val state = _uiState.value
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, ProcrastinationTrackerApp.TRACKER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BRAND_COLOR)
            .setContentIntent(openAppIntent)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)

        if (state.isTracking) {
            builder
                .setContentTitle(state.activeSliceTitle)
                .setContentText(getString(R.string.tracker_notification_running))
                // The system renders the running time, so the service doesn't repost every second.
                .setUsesChronometer(true)
                .setWhen(startedAtWallClock)
                .setShowWhen(true)
                .setOngoing(true)
                .addAction(0, getString(R.string.tracker_notification_action_pause), serviceAction(2, ACTION_PAUSE))
                .addAction(0, getString(R.string.tracker_notification_action_switch), serviceAction(3, ACTION_NEXT_SLICE))
                .addAction(0, getString(R.string.tracker_notification_action_stop), serviceAction(1, ACTION_STOP))
        } else {
            builder
                .setContentTitle(getString(R.string.tracker_notification_idle_title))
                .setContentText(
                    lastSliceTitle.takeIf { it.isNotBlank() }
                        ?.let { getString(R.string.tracker_notification_idle_last, it) }
                        ?: getString(R.string.tracker_notification_idle_text)
                )
                .setUsesChronometer(false)
                .setShowWhen(false)
                .setOngoing(false)
                .addAction(0, getString(R.string.tracker_notification_action_resume), serviceAction(4, ACTION_RESUME))
                .addAction(0, getString(R.string.tracker_notification_action_stop), serviceAction(1, ACTION_STOP))
        }
        return builder.build()
    }

    /**
     * The bubble mirrors the notification's lifetime: it appears while something is being tracked
     * and goes away when nothing is. Kept next to the notification update so the two can never
     * disagree about whether a block is running.
     */
    private fun updateBubble() {
        val state = _uiState.value
        if (state.isTracking) {
            BubbleController.show(this, state.activeSliceTitle.orEmpty(), state.elapsedMillis)
        } else {
            BubbleController.hide(this)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
        updateBubble()
        TrackerTileService.refresh(this)
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
