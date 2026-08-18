package com.foxlab.procrastinationtracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.foxlab.procrastinationtracker.ProcrastinationTrackerApp
import com.foxlab.procrastinationtracker.core.Session
import com.foxlab.procrastinationtracker.core.TimerDaySummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as ProcrastinationTrackerApp).repository

    data class UiState(
        val today: List<Session> = emptyList(),
        val focusedTodayMillis: Long = 0L,
        val procrastinatedTodayMillis: Long = 0L,
        val allHistory: List<Session> = emptyList(),
        /** Same cycle summary the Timer screen shows live. */
        val summary: TimerDaySummary = TimerDaySummary()
    )

    val uiState = combine(
        repository.observeToday(),
        repository.observeAll(),
        repository.observeTodaySummary()
    ) { today, all, summary ->
        val (focused, procrastinated) = repository.totals(today)
        UiState(
            today = today,
            focusedTodayMillis = focused,
            procrastinatedTodayMillis = procrastinated,
            allHistory = all,
            summary = summary
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())
}
