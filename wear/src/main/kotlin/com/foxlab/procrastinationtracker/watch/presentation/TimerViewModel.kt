package com.foxlab.procrastinationtracker.watch.presentation

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.foxlab.procrastinationtracker.core.TimerMode
import com.foxlab.procrastinationtracker.watch.service.TimerForegroundService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TimerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val uiState = TimerForegroundService.uiState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TimerForegroundService.uiState.value
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

    private fun sendAction(action: String) {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(context, intent)
    }
}
