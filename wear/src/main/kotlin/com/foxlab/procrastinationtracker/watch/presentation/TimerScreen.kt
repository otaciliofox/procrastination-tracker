package com.foxlab.procrastinationtracker.watch.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.foxlab.procrastinationtracker.core.Phase
import com.foxlab.procrastinationtracker.core.minutesToMillis
import com.foxlab.procrastinationtracker.core.toClockString

/**
 * Timer mode on the watch, in the same language as the phone: focus is blue, the interval is teal
 * (never rose -- that belongs to procrastination in the Tracker), and the primary control is
 * filled while running, outlined while idle. The round progress ring is the watch-specific part.
 */
@Composable
fun TimerScreen(onOpenModeSelect: () -> Unit, viewModel: TimerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val accent = if (state.phase == Phase.FOCUS) WatchPalette.Blue else WatchPalette.Teal
    val context = LocalContext.current

    // Asked here, on the first start, and never at launch: this is the only screen whose whole
    // point depends on being able to buzz your wrist when the block ends.
    var startAfterPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (startAfterPermission) {
            startAfterPermission = false
            viewModel.start()
        }
    }
    val onStart: () -> Unit = {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.start()
        } else {
            startAfterPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val phaseDurationMillis = when (state.phase) {
        Phase.FOCUS -> state.mode.focusMinutes
        Phase.SHORT_BREAK -> state.mode.shortBreakMinutes
        Phase.LONG_BREAK -> state.mode.longBreakMinutes
    }.minutesToMillis()
    val progress = if (phaseDurationMillis == 0L) 0f else
        1f - (state.remainingMillis.coerceIn(0, phaseDurationMillis).toFloat() / phaseDurationMillis)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WatchPalette.CanvasTop, WatchPalette.Canvas))),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            indicatorColor = accent,
            trackColor = WatchPalette.OutlineSoft
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.mode.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onOpenModeSelect() }
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.remainingMillis.toClockString(),
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                color = WatchPalette.Ink
            )
            Text(
                text = when (state.phase) {
                    Phase.FOCUS -> "FOCO"
                    Phase.SHORT_BREAK -> "INTERVALO"
                    Phase.LONG_BREAK -> "INTERVALO LONGO"
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = WatchPalette.InkMuted
            )

            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable { viewModel.reset() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = "Reiniciar",
                        tint = WatchPalette.InkMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (state.isRunning) accent else accent.copy(alpha = 0.20f))
                        .border(
                            1.5.dp,
                            if (state.isRunning) Color.Transparent else accent.copy(alpha = 0.6f),
                            CircleShape
                        )
                        .clickable { if (state.isRunning) viewModel.pause() else onStart() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isRunning) "Pausar" else "Iniciar",
                        tint = if (state.isRunning) Color.White else accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
