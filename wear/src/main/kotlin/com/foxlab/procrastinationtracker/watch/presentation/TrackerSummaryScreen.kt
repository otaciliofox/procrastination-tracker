package com.foxlab.procrastinationtracker.watch.presentation

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Watch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.wear.compose.material.Icon
import com.foxlab.procrastinationtracker.trackerdata.SOURCE_PHONE
import com.foxlab.procrastinationtracker.trackerdata.SOURCE_WATCH
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Text
import com.foxlab.procrastinationtracker.core.toHoursMinutesString
import com.foxlab.procrastinationtracker.core.toStopwatchString

/**
 * "Hoje" only on the watch -- the weekly report stays on the phone, where there's room for it.
 * Each activity keeps its color and shows its share of the day as a bar, same reading as the
 * phone's board without pretending a 1.4" screen can hold the same chart.
 */
@Composable
fun TrackerSummaryScreen(viewModel: TrackerViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Tudo / Relógio / Celular. The watch keeps the same "where did this come from" question the
    // phone's report answers with its own tab -- just sized for a wrist.
    var filter by remember { mutableStateOf(DeviceFilter.ALL) }

    val millisFor: (String) -> Long = { sliceId ->
        when (filter) {
            DeviceFilter.ALL -> state.liveTodayTotal(sliceId)
            DeviceFilter.WATCH -> state.todayByDevice[SOURCE_WATCH]?.get(sliceId) ?: 0L
            DeviceFilter.PHONE -> state.todayByDevice[SOURCE_PHONE]?.get(sliceId) ?: 0L
        }
    }
    val dayTotal = state.slices.sumOf { millisFor(it.id) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WatchPalette.CanvasTop, WatchPalette.Canvas)))
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "HOJE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = WatchPalette.InkMuted
                    )
                    Text(
                        text = dayTotal.toStopwatchString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Light,
                        color = WatchPalette.Ink
                    )
                }
            }

            item {
                DeviceFilterRow(selected = filter, onSelect = { filter = it })
            }

            items(state.slices) { slice ->
                val index = state.slices.indexOf(slice)
                val millis = millisFor(slice.id)
                val color = WatchPalette.activityColor(slice.title, index)
                val share = if (dayTotal > 0) (millis.toFloat() / dayTotal.toFloat()).coerceIn(0f, 1f) else 0f

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = slice.title,
                            fontSize = 12.sp,
                            color = WatchPalette.Ink,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = millis.toHoursMinutesString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WatchPalette.Ink
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(WatchPalette.OutlineSoft)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(share)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                        )
                    }
                }
            }

            // The watch keeps a 30-day window on purpose; the full history and any editing live
            // on the phone, so this is the honest exit instead of a cramped half-feature.
            item {
                WatchListRow(
                    title = "Ver tudo no celular",
                    accent = WatchPalette.Teal,
                    onClick = { OpenOnPhone.launch(context) }
                )
            }
        }
    }
}

private enum class DeviceFilter { ALL, WATCH, PHONE }

/** Three small buttons: everything, only this watch, only the phone. */
@Composable
private fun DeviceFilterRow(selected: DeviceFilter, onSelect: (DeviceFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip("Tudo", null, selected == DeviceFilter.ALL) { onSelect(DeviceFilter.ALL) }
        FilterChip(null, Icons.Filled.Watch, selected == DeviceFilter.WATCH) { onSelect(DeviceFilter.WATCH) }
        FilterChip(null, Icons.Filled.Smartphone, selected == DeviceFilter.PHONE) { onSelect(DeviceFilter.PHONE) }
    }
}

@Composable
private fun FilterChip(label: String?, icon: ImageVector?, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) WatchPalette.Blue.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) WatchPalette.Blue else WatchPalette.InkMuted,
                modifier = Modifier.size(14.dp)
            )
        } else {
            Text(
                text = label.orEmpty(),
                fontSize = 10.sp,
                color = if (selected) WatchPalette.Blue else WatchPalette.InkMuted
            )
        }
    }
}
