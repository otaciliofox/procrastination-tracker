package com.foxlab.procrastinationtracker.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.foxlab.procrastinationtracker.ProcrastinationTrackerApp
import com.foxlab.procrastinationtracker.core.TimerMode
import com.foxlab.procrastinationtracker.core.TimerPlan
import com.foxlab.procrastinationtracker.trackerdata.settings.CustomPlanStore
import com.foxlab.procrastinationtracker.core.TimerDaySummary
import com.foxlab.procrastinationtracker.service.TimerForegroundService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class TimerViewModel(app: Application) : AndroidViewModel(app) {

    private val sessions = (app as ProcrastinationTrackerApp).repository

    data class UiState(
        val timer: TimerForegroundService.UiState,
        /** Today's cycles -- the Timer mode's report is a summary, not a weekly chart. */
        val summary: TimerDaySummary = TimerDaySummary()
    )

    val uiState = combine(
        TimerForegroundService.uiState,
        sessions.observeTodaySummary()
    ) { timer, summary ->
        UiState(timer, summary)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UiState(TimerForegroundService.uiState.value)
    )

    fun start() = sendAction(TimerForegroundService.ACTION_START)
    fun pause() = sendAction(TimerForegroundService.ACTION_PAUSE)
    fun reset() = sendAction(TimerForegroundService.ACTION_RESET)

    fun setMode(mode: TimerMode) {
        val intent = Intent(getApplication(), TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_SET_MODE
            putExtra(TimerForegroundService.EXTRA_MODE, mode.name)
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun customPlan(): TimerPlan = CustomPlanStore.load(getApplication())

    /** Saves the user's own durations and puts the engine on them right away. */
    fun saveCustomPlan(plan: TimerPlan) {
        CustomPlanStore.save(getApplication(), plan)
        sendAction(TimerForegroundService.ACTION_APPLY_CUSTOM_PLAN)
    }

    private fun sendAction(action: String) {
        val intent = Intent(getApplication(), TimerForegroundService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }
}
