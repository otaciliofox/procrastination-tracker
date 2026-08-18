package com.foxlab.procrastinationtracker.watch.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import com.foxlab.procrastinationtracker.core.TimerMode

/**
 * Preset picker. The Custom mode is deliberately absent here: its durations are configured on the
 * phone (typing four numbers on a 1.4" screen is worse than the problem it solves), and the watch
 * simply runs whatever preset you choose.
 */
@Composable
fun ModeSelectScreen(onModeChosen: () -> Unit, viewModel: TimerViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val presets = TimerMode.entries.filter { it != TimerMode.CUSTOM }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WatchPalette.CanvasTop, WatchPalette.Canvas)))
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { WatchScreenTitle("Modo") }
            items(presets) { mode ->
                WatchListRow(
                    title = mode.label,
                    subtitle = "${mode.focusMinutes}min foco / ${mode.shortBreakMinutes}min intervalo",
                    accent = WatchPalette.Blue,
                    selected = state.mode == mode,
                    onClick = {
                        viewModel.setMode(mode)
                        onModeChosen()
                    }
                )
            }
        }
    }
}
