package com.foxlab.procrastinationtracker.watch.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.foxlab.procrastinationtracker.core.Phase
import com.foxlab.procrastinationtracker.core.Session
import com.foxlab.procrastinationtracker.core.TimerEngine
import com.foxlab.procrastinationtracker.core.TimerMode
import com.foxlab.procrastinationtracker.core.toClockString
import com.foxlab.procrastinationtracker.watch.R
import com.foxlab.procrastinationtracker.watch.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Watch-side twin of the phone's TimerForegroundService: same TimerEngine from
 * :core, but on phase completion it pushes the session to the phone instead of
 * (or in addition to) writing to a local Room DB, since the watch has no need
 * for its own persisted history.
 */
class TimerForegroundService : Service() {

    data class UiState(
        val mode: TimerMode,
        val phase: Phase,
        val remainingMillis: Long,
        val isRunning: Boolean,
        val focusStreak: Int
    )

    companion object {
        const val ACTION_START = "com.foxlab.procrastinationtracker.watch.action.START"
        const val ACTION_PAUSE = "com.foxlab.procrastinationtracker.watch.action.PAUSE"
        const val ACTION_RESET = "com.foxlab.procrastinationtracker.watch.action.RESET"
        const val ACTION_SET_MODE = "com.foxlab.procrastinationtracker.watch.action.SET_MODE"
        const val EXTRA_MODE = "extra_mode"
        const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 42

        private val engine = TimerEngine(TimerMode.FIFTY_TWO_SEVENTEEN)
        private val _uiState = MutableStateFlow(
            UiState(engine.mode, engine.phase, engine.remainingMillis, engine.isRunning, engine.focusStreak)
        )
        val uiState: StateFlow<UiState> = _uiState.asStateFlow()

        private fun publish() {
            _uiState.value = UiState(
                mode = engine.mode,
                phase = engine.phase,
                remainingMillis = engine.remainingMillis,
                isRunning = engine.isRunning,
                focusStreak = engine.focusStreak
            )
        }
    }

    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        val channel = android.app.NotificationChannel(
            CHANNEL_ID, "Timer", android.app.NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                engine.start(SystemClock.elapsedRealtime())
                startTicking()
            }
            ACTION_PAUSE -> {
                engine.pause()
                publish()
            }
            ACTION_RESET -> {
                engine.reset()
                publish()
            }
            ACTION_SET_MODE -> {
                val modeName = intent.getStringExtra(EXTRA_MODE) ?: TimerMode.FIFTY_TWO_SEVENTEEN.name
                engine.changeMode(TimerMode.fromName(modeName))
                publish()
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun startTicking() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (engine.isRunning) {
                delay(1000)
                if (!engine.isRunning) break
                val event = engine.tick(1000)
                if (event is TimerEngine.Event.PhaseCompleted) {
                    onPhaseCompleted(event)
                }
                publish()
                updateNotification()
            }
        }
    }

    private fun onPhaseCompleted(event: TimerEngine.Event.PhaseCompleted) {
        val now = System.currentTimeMillis()
        val durationMillis = when (event.finishedPhase) {
            Phase.FOCUS -> engine.mode.focusMinutes
            Phase.SHORT_BREAK -> engine.mode.shortBreakMinutes
            Phase.LONG_BREAK -> engine.mode.longBreakMinutes
        } * 60_000L
        val session = Session(
            mode = engine.mode,
            phase = event.finishedPhase,
            startTimeMillis = now - durationMillis,
            endTimeMillis = now,
            completedFully = true,
            source = "watch"
        )
        WearSyncSender.sendSession(applicationContext, session)
    }

    private fun buildNotification(): Notification {
        val state = _uiState.value
        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val phaseLabel = if (state.phase == Phase.FOCUS) "Foco" else "Pausa"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("$phaseLabel · ${state.mode.label}")
            .setContentText(state.remainingMillis.toClockString())
            .setContentIntent(openAppIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(state.isRunning)
            .build()
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
