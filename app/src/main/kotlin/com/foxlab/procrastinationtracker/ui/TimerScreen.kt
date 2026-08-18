package com.foxlab.procrastinationtracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.foxlab.procrastinationtracker.R
import com.foxlab.procrastinationtracker.core.Phase
import com.foxlab.procrastinationtracker.core.TimerMode
import com.foxlab.procrastinationtracker.core.toClockString
import com.foxlab.procrastinationtracker.core.toHoursMinutesString
import com.foxlab.procrastinationtracker.core.TimerDaySummary
import com.foxlab.procrastinationtracker.ui.theme.BoardPalette
import com.foxlab.procrastinationtracker.viewmodel.TimerViewModel

/**
 * Timer mode in the board's language: canvas, one accent per phase (focus blue / break rose),
 * light oversized type, and the same outlined-vs-filled logic on the primary control.
 *
 * There is no weekly report here on purpose -- a countdown's history is "how many cycles did I
 * finish today", which is the summary card under the clock.
 */
@Composable
fun TimerScreen(viewModel: TimerViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val timer = state.timer
    // Focus is blue, interval is teal. Rose belongs to procrastination in the Tracker and
    // must never leak into the Timer, where a break is time the user chose to take.
    val accent = if (timer.phase == Phase.FOCUS) BoardPalette.Blue else BoardPalette.Teal

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showCustomEditor by remember { mutableStateOf(false) }
    var startAfterPermission by remember { mutableStateOf(false) }

    val notifications = rememberNotificationPermission {
        if (startAfterPermission) {
            startAfterPermission = false
            viewModel.start()
        }
    }

    val onPlayPause: () -> Unit = {
        if (timer.isRunning) {
            viewModel.pause()
        } else if (notifications.isGranted || !notifications.isSupported) {
            viewModel.start()
        } else {
            showPermissionDialog = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BoardPalette.CanvasTop, BoardPalette.Canvas)))
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        ModeChips(
            selected = timer.mode,
            accent = accent,
            onSelect = { viewModel.setMode(it) },
            onEditCustom = { showCustomEditor = true }
        )

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(listOf(accent.copy(alpha = 0.20f), accent.copy(alpha = 0.08f)))
                )
                .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(28.dp))
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when (timer.phase) {
                        Phase.FOCUS -> "FOCO"
                        Phase.SHORT_BREAK -> "INTERVALO"
                        Phase.LONG_BREAK -> "INTERVALO LONGO"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = accent
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = timer.remainingMillis.toClockString(),
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-2).sp,
                    color = BoardPalette.Ink
                )
                if (timer.plan.hasLongBreak) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "${timer.focusStreak % timer.plan.focusSessionsBeforeLongBreak}/${timer.plan.focusSessionsBeforeLongBreak} até o intervalo longo",
                        fontSize = 13.sp,
                        color = BoardPalette.InkMuted
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        TimerDaySummaryCard(summary = state.summary)

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { viewModel.reset() },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = "Reiniciar",
                        tint = BoardPalette.InkMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.size(28.dp))

            Surface(
                onClick = onPlayPause,
                shape = CircleShape,
                color = if (timer.isRunning) accent else accent.copy(alpha = 0.18f),
                border = if (timer.isRunning) null else BorderStroke(1.5.dp, accent.copy(alpha = 0.55f)),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (timer.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (timer.isRunning) "Pausar" else "Iniciar",
                        tint = if (timer.isRunning) Color.White else accent,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.timer_permission_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.timer_permission_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    startAfterPermission = true
                    notifications.request()
                }) { Text(stringResource(R.string.timer_permission_allow)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    viewModel.start()
                }) { Text(stringResource(R.string.timer_permission_skip)) }
            }
        )
    }

    if (showCustomEditor) {
        CustomPlanDialog(
            plan = viewModel.customPlan(),
            onDismiss = { showCustomEditor = false },
            onSave = { plan ->
                viewModel.saveCustomPlan(plan)
                showCustomEditor = false
            }
        )
    }
}

@Composable
private fun ModeChips(
    selected: TimerMode,
    accent: Color,
    onSelect: (TimerMode) -> Unit,
    onEditCustom: () -> Unit
) {
    val presets = listOf(TimerMode.FIFTY_TWO_SEVENTEEN, TimerMode.POMODORO, TimerMode.FORTY_FIVE_FIFTEEN)
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { mode ->
                ModeChip(
                    label = mode.label,
                    selected = selected == mode,
                    accent = accent,
                    onClick = { onSelect(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // Tapping anywhere on the chip picks Custom *and* opens the editor: choosing "meu tempo"
        // and then having to find a pencil to say which time was a dead end.
        ModeChip(
            label = TimerMode.CUSTOM.label,
            selected = selected == TimerMode.CUSTOM,
            accent = accent,
            onClick = {
                onSelect(TimerMode.CUSTOM)
                onEditCustom()
            },
            trailing = {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.timer_custom_edit),
                    tint = if (selected == TimerMode.CUSTOM) accent else BoardPalette.InkMuted,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, if (selected) accent.copy(alpha = 0.5f) else BoardPalette.OutlineSoft, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accent else BoardPalette.InkMuted
        )
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

