package com.foxlab.procrastinationtracker.watch.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.foxlab.procrastinationtracker.core.toStopwatchString
import com.foxlab.procrastinationtracker.trackerdata.LiveSessionState
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity

/**
 * The phone's board, rewritten for a round screen instead of copied onto one: the stacked bands
 * become a scaling list (which is what a watch does well, and what keeps 2 to 10 activities
 * equally usable), the day headline is the first item, and the controls are the last one so they
 * never sit on top of an activity.
 *
 * Colors and the procrastination rule come from `:core`; only this layout is watch-specific.
 */
@Composable
fun TrackerScreen(
    onOpenProfiles: () -> Unit,
    onOpenSummary: () -> Unit,
    viewModel: TrackerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showBackMenu by remember { mutableStateOf(false) }
    val listState = rememberScalingLazyListState()

    // Raising your wrist while the phone is counting: same three answers as the phone offers.
    var handoff by remember { mutableStateOf<LiveSessionState?>(null) }
    var handoffDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(state.service.isTracking, state.slices) {
        handoff = if (handoffDismissed) null else viewModel.remoteRunningSession()
    }

    handoff?.let { remote ->
        HandoffScreen(
            remote = remote,
            onContinueHere = { viewModel.takeOverRemoteSession(remote); handoff = null },
            onStartNew = {
                state.slices.firstOrNull()?.let { viewModel.closeRemoteAndStartNew(remote, it) }
                handoff = null
            },
            onKeepThere = { handoffDismissed = true; handoff = null }
        )
        return
    }

    if (showBackMenu) {
        TrackerBackMenuScreen(
            onPause = { viewModel.pauseActive(); showBackMenu = false },
            onStop = { viewModel.stopActive(); showBackMenu = false },
            onRestart = { viewModel.discardActive(); showBackMenu = false },
            onExitApp = { viewModel.stopActive(); showBackMenu = false }
        )
        return
    }

    val slices = state.slices
    val activeId = state.service.activeSliceId
    val millisFor: (ActivitySliceEntity) -> Long = { state.liveTodayTotal(it.id) }
    val dayTotal = slices.sumOf(millisFor)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WatchPalette.CanvasTop, WatchPalette.Canvas)))
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                DayHeadline(
                    totalMillis = dayTotal,
                    isTracking = state.service.isTracking,
                    runningTitle = slices.firstOrNull { it.id == activeId }?.title,
                    profileTitle = state.activeProfile?.title.orEmpty(),
                    onProfileTap = onOpenProfiles
                )
            }

            items(slices) { slice ->
                val index = slices.indexOf(slice)
                ActivityRow(
                    title = slice.title,
                    iconKey = slice.iconKey,
                    millis = millisFor(slice),
                    color = WatchPalette.activityColor(slice.title, index),
                    isActive = slice.id == activeId,
                    isRunning = slice.id == activeId && state.service.isTracking,
                    onTap = {
                        if (slice.id == activeId) viewModel.pauseActive() else viewModel.activateSlice(slice)
                    }
                )
            }

            item {
                ControlRow(
                    isTracking = state.service.isTracking,
                    accent = slices.indexOfFirst { it.id == activeId }
                        .takeIf { it >= 0 }
                        ?.let { WatchPalette.activityColor(slices[it].title, it) }
                        ?: WatchPalette.Blue,
                    onPlayPause = {
                        if (state.service.isTracking) viewModel.pauseActive()
                        else slices.firstOrNull()?.let { viewModel.activateSlice(it) }
                    },
                    onOptions = { showBackMenu = true },
                    onSummary = onOpenSummary
                )
            }
        }
    }
}

@Composable
private fun DayHeadline(
    totalMillis: Long,
    isTracking: Boolean,
    runningTitle: String?,
    profileTitle: String,
    onProfileTap: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HOJE",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = WatchPalette.InkMuted
        )
        Text(
            text = totalMillis.toStopwatchString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Light,
            color = WatchPalette.Ink
        )
        Spacer(Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(onClick = onProfileTap)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            if (isTracking) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(WatchPalette.Green)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = runningTitle ?: profileTitle,
                fontSize = 10.sp,
                color = if (runningTitle != null) WatchPalette.Ink else WatchPalette.InkMuted
            )
        }
    }
}

/**
 * One activity on the wrist: color rail, **icon** and time. Names are the phone's job -- at four
 * to six activities on a 1.4" screen a word either truncates or shrinks past reading, while the
 * icon plus its color stays recognisable. The name is still announced to accessibility, and the
 * running activity is spelled out in the headline above.
 */
@Composable
private fun ActivityRow(
    title: String,
    iconKey: String?,
    millis: Long,
    color: Color,
    isActive: Boolean,
    isRunning: Boolean,
    onTap: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(
                if (isActive) Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.75f)))
                else Brush.horizontalGradient(listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.10f)))
            )
            .border(1.dp, if (isActive) Color.Transparent else color.copy(alpha = 0.25f), shape)
            .semantics { contentDescription = title }
            .clickable(onClick = onTap)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(if (isActive) Color.White.copy(alpha = 0.6f) else color)
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = WatchActivityIcons.iconFor(title, iconKey),
                contentDescription = null,
                tint = if (isActive) Color.White else WatchPalette.Ink.copy(alpha = 0.9f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = millis.toStopwatchString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                color = if (isActive) Color.White else WatchPalette.Ink,
                modifier = Modifier.weight(1f)
            )
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun ControlRow(
    isTracking: Boolean,
    accent: Color,
    onPlayPause: () -> Unit,
    onOptions: () -> Unit,
    onSummary: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GhostButton(Icons.Filled.MoreHoriz, "Opções", onOptions)
        PrimaryButton(isTracking = isTracking, accent = accent, onClick = onPlayPause)
        GhostButton(Icons.Filled.Insights, "Hoje", onSummary)
    }
}

@Composable
private fun PrimaryButton(isTracking: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isTracking) accent else accent.copy(alpha = 0.20f))
            .border(1.5.dp, if (isTracking) Color.Transparent else accent.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isTracking) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isTracking) "Pausar" else "Iniciar",
            tint = if (isTracking) Color.White else accent,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun GhostButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = WatchPalette.Ink.copy(alpha = 0.85f),
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * The wrist half of the hand-off. Same three answers as the phone's dialog, laid out as rows
 * because a watch has no room for a dialog with three buttons.
 */
@Composable
private fun HandoffScreen(
    remote: LiveSessionState,
    onContinueHere: () -> Unit,
    onStartNew: () -> Unit,
    onKeepThere: () -> Unit
) {
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
                        text = "O celular está rastreando",
                        fontSize = 11.sp,
                        color = WatchPalette.InkMuted
                    )
                    Text(
                        text = remote.sliceTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WatchPalette.Ink
                    )
                    Text(
                        text = remote.elapsedMillis().toStopwatchString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = WatchPalette.Ink
                    )
                }
            }
            item { WatchListRow(title = "Continuar aqui", accent = WatchPalette.Blue, onClick = onContinueHere) }
            item { WatchListRow(title = "Começar um novo", accent = WatchPalette.Teal, onClick = onStartNew) }
            item { WatchListRow(title = "Deixar no celular", accent = WatchPalette.InkMuted, onClick = onKeepThere) }
        }
    }
}
