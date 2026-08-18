package com.foxlab.procrastinationtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxlab.procrastinationtracker.data.SessionRepository
import com.foxlab.procrastinationtracker.core.Session
import com.foxlab.procrastinationtracker.core.TimerDaySummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: SessionRepository
) : ViewModel() {

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
