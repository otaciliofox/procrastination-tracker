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
import com.foxlab.procrastinationtracker.trackerdata.entity.LayoutProfileEntity

/**
 * Only lets you *switch* between profiles that already exist. Creating/editing/deleting a Custom
 * profile stays on the phone -- the tiny round screen isn't a good place to type several activity
 * names. Whatever you set up there syncs here (spec 002 §7).
 */
@Composable
fun TrackerProfileSelectScreen(onProfileChosen: () -> Unit, viewModel: TrackerViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WatchPalette.CanvasTop, WatchPalette.Canvas)))
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { WatchScreenTitle("Perfil") }
            items(state.profiles) { profile: LayoutProfileEntity ->
                WatchListRow(
                    title = profile.title,
                    subtitle = state.activityCountByProfile[profile.id]
                        ?.takeIf { it > 0 }
                        ?.let { "$it atividades" },
                    accent = WatchPalette.Blue,
                    selected = profile.id == state.activeProfile?.id,
                    onClick = {
                        viewModel.switchProfile(profile.id)
                        onProfileChosen()
                    }
                )
            }
        }
    }
}
