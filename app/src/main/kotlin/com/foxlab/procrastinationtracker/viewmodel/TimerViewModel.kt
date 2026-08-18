package com.foxlab.procrastinationtracker.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxlab.procrastinationtracker.data.SessionRepository
import com.foxlab.procrastinationtracker.core.TimerMode
import com.foxlab.procrastinationtracker.core.TimerPlan
import com.foxlab.procrastinationtracker.trackerdata.settings.CustomPlanStore
import com.foxlab.procrastinationtracker.core.TimerDaySummary
import com.foxlab.procrastinationtracker.service.TimerForegroundService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val sessions: SessionRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

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
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_SET_MODE
            putExtra(TimerForegroundService.EXTRA_MODE, mode.name)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun customPlan(): TimerPlan = CustomPlanStore.load(context)

    /** Saves the user's own durations and puts the engine on them right away. */
    fun saveCustomPlan(plan: TimerPlan) {
        CustomPlanStore.save(context, plan)
        sendAction(TimerForegroundService.ACTION_APPLY_CUSTOM_PLAN)
    }

    private fun sendAction(action: String) {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(context, intent)
    }
}
