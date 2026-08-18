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
import com.foxlab.procrastinationtracker.core.Phase
import com.foxlab.procrastinationtracker.core.Session
import com.foxlab.procrastinationtracker.core.TimerEngine
import com.foxlab.procrastinationtracker.core.TimerMode
import com.foxlab.procrastinationtracker.core.TimerPlan
import com.foxlab.procrastinationtracker.core.toClockString
import com.foxlab.procrastinationtracker.trackerdata.settings.CustomPlanStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the single [TimerEngine] instance for the phone app, ticks it once a second,
 * shows a persistent notification with the remaining time, and writes finished
 * sessions to Room. UI observes [uiState] instead of binding to the service.
 */
@AndroidEntryPoint
class TimerForegroundService : Service() {

    data class UiState(
        val mode: TimerMode,
        val plan: TimerPlan,
        val phase: Phase,
        val remainingMillis: Long,
        val isRunning: Boolean,
        val focusStreak: Int
    )

    companion object {
        const val ACTION_START = "com.foxlab.procrastinationtracker.action.START"
        const val ACTION_PAUSE = "com.foxlab.procrastinationtracker.action.PAUSE"
        const val ACTION_RESET = "com.foxlab.procrastinationtracker.action.RESET"
        const val ACTION_SET_MODE = "com.foxlab.procrastinationtracker.action.SET_MODE"
        const val ACTION_APPLY_CUSTOM_PLAN = "com.foxlab.procrastinationtracker.action.APPLY_CUSTOM_PLAN"
        const val EXTRA_MODE = "extra_mode"
        /** Tints the glyph in the shade with the app's blue. */
        private const val BRAND_COLOR = 0xFF3B82F6.toInt()
        private const val NOTIFICATION_ID = 42
        private const val ALERT_NOTIFICATION_ID = 44

        /** Below this, an interrupted block is noise rather than history. */
        private const val MIN_PARTIAL_MILLIS = 60_000L

        private val engine = TimerEngine(TimerMode.FIFTY_TWO_SEVENTEEN)
        private val _uiState = MutableStateFlow(
            UiState(engine.mode, engine.plan, engine.phase, engine.remainingMillis, engine.isRunning, engine.focusStreak)
        )
        val uiState: StateFlow<UiState> = _uiState.asStateFlow()

        private fun publish() {
            _uiState.value = UiState(
                mode = engine.mode,
                plan = engine.plan,
                phase = engine.phase,
                remainingMillis = engine.remainingMillis,
                isRunning = engine.isRunning,
                focusStreak = engine.focusStreak
            )
        }
    }

    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob())
    @Inject lateinit var repository: com.foxlab.procrastinationtracker.data.SessionRepository

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                engine.start(SystemClock.elapsedRealtime())
                publish()
                startTicking()
            }
            ACTION_PAUSE -> {
                // An interrupted block is still time you spent -- save it before the clock stops.
                scope.launch { recordPartialBlock() }
                engine.pause()
                publish()
            }
            ACTION_RESET -> {
                scope.launch { recordPartialBlock() }
                engine.reset()
                publish()
            }
            ACTION_SET_MODE -> {
                scope.launch { recordPartialBlock() }
                val modeName = intent.getStringExtra(EXTRA_MODE) ?: TimerMode.FIFTY_TWO_SEVENTEEN.name
                engine.changePlan(CustomPlanStore.planFor(this, TimerMode.fromName(modeName)))
                publish()
            }
            ACTION_APPLY_CUSTOM_PLAN -> {
                scope.launch { recordPartialBlock() }
                engine.changePlan(CustomPlanStore.load(this))
                publish()
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    /** The in-app clock ticks every second; the notification only changes when the phase does. */
    private fun startTicking() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (engine.isRunning) {
                delay(1000)
                if (!engine.isRunning) break
                val event = engine.tick(1000)
                publish()
                if (event is TimerEngine.Event.PhaseCompleted) {
                    onPhaseCompleted(event)
                    updateNotification()
                }
            }
        }
    }

    /**
     * Pausing, resetting or switching modes mid-block used to throw that time away, which made
     * "how long did I actually focus today" wrong. It is now saved as an incomplete session:
     * counted in the totals, not counted as a finished cycle.
     */
    private suspend fun recordPartialBlock() {
        if (!engine.isRunning) return
        val elapsed = engine.elapsedInPhaseMillis
        if (elapsed < MIN_PARTIAL_MILLIS) return
        val now = System.currentTimeMillis()
        repository.record(
            Session(
                mode = engine.mode,
                phase = engine.phase,
                startTimeMillis = now - elapsed,
                endTimeMillis = now,
                completedFully = false,
                source = "phone"
            )
        )
    }

    private suspend fun onPhaseCompleted(event: TimerEngine.Event.PhaseCompleted) {
        val now = System.currentTimeMillis()
        val durationMillis = engine.durationFor(event.finishedPhase)
        repository.record(
            Session(
                mode = engine.mode,
                phase = event.finishedPhase,
                startTimeMillis = now - durationMillis,
                endTimeMillis = now,
                completedFully = true,
                source = "phone"
            )
        )
        alertPhaseCompleted(event)
    }

    /**
     * The whole point of a pre-configured mode: it has to tell you the block ended, even with the
     * screen off and the app closed. Separate high-importance channel, because the ongoing
     * countdown notification is deliberately silent.
     */
    private fun alertPhaseCompleted(event: TimerEngine.Event.PhaseCompleted) {
        val title = when (event.finishedPhase) {
            Phase.FOCUS -> getString(R.string.timer_alert_focus_done)
            Phase.SHORT_BREAK, Phase.LONG_BREAK -> getString(R.string.timer_alert_break_done)
        }
        val nextMinutes = (engine.durationFor(event.nextPhase) / 60_000L).toInt()
        val text = when (event.nextPhase) {
            Phase.FOCUS -> getString(R.string.timer_alert_next_focus, nextMinutes)
            Phase.SHORT_BREAK, Phase.LONG_BREAK -> getString(R.string.timer_alert_next_break, nextMinutes)
        }
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alert = NotificationCompat.Builder(this, ProcrastinationTrackerApp.TIMER_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BRAND_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(ALERT_NOTIFICATION_ID, alert)
    }

    private fun serviceAction(requestCode: Int, action: String): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, TimerForegroundService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /**
     * Same idea as the tracker's: the countdown is rendered by the system (chronometer running
     * backwards to the end of the block), so the service only re-posts on state changes, and the
     * controls are right there instead of only inside the app.
     */
    private fun buildNotification(): Notification {
        val state = _uiState.value
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val phaseLabel = when (state.phase) {
            Phase.FOCUS -> getString(R.string.phase_focus)
            Phase.SHORT_BREAK, Phase.LONG_BREAK -> getString(R.string.phase_break)
        }

        val builder = NotificationCompat.Builder(this, ProcrastinationTrackerApp.TIMER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BRAND_COLOR)
            .setContentTitle("$phaseLabel · ${state.mode.label}")
            .setContentIntent(openAppIntent)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(state.isRunning)
            .addAction(0, getString(R.string.tracker_menu_restart), serviceAction(3, ACTION_RESET))

        if (state.isRunning) {
            builder
                .setContentText(getString(R.string.timer_notification_running))
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + state.remainingMillis)
                .setShowWhen(true)
                .addAction(0, getString(R.string.tracker_notification_action_pause), serviceAction(1, ACTION_PAUSE))
        } else {
            builder
                .setContentText(getString(R.string.timer_notification_paused, state.remainingMillis.toClockString()))
                .setUsesChronometer(false)
                .setShowWhen(false)
                .addAction(0, getString(R.string.tracker_notification_action_resume), serviceAction(2, ACTION_START))
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
