package com.foxlab.procrastinationtracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.foxlab.procrastinationtracker.core.toStopwatchString
import com.foxlab.procrastinationtracker.ui.theme.BoardPalette
import com.foxlab.procrastinationtracker.ui.theme.ProcrastinationTrackerTheme
import com.foxlab.procrastinationtracker.viewmodel.TrackerViewModel

/**
 * The floating bubble's content: the smallest useful version of the board.
 *
 * It exists for the case the whole app is built around -- you left for Instagram or Duolingo and
 * the clock is still running. From here you can see what is counting, switch activity, or pause,
 * without leaving the app you are actually in. Anything more (reports, renaming, corrections)
 * belongs in the full screen, one tap away.
 */
class BubbleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProcrastinationTrackerTheme {
                BubbleContent()
            }
        }
    }
}

@Composable
private fun BubbleContent(viewModel: TrackerViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val service = state.service
    val activeIndex = state.slices.indexOfFirst { it.id == service.activeSliceId }
    val accent = if (activeIndex >= 0) {
        activityColor(state.slices[activeIndex].title, activeIndex)
    } else {
        BoardPalette.Blue
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BoardPalette.CanvasTop, BoardPalette.Canvas)))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (service.isTracking) "RASTREANDO" else "PAUSADO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = if (service.isTracking) BoardPalette.Green else BoardPalette.InkMuted
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = service.activeSliceTitle
                    ?: state.slices.firstOrNull()?.title.orEmpty(),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = BoardPalette.Ink
            )
            Text(
                text = service.elapsedMillis.toStopwatchString(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Light,
                color = BoardPalette.Ink
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        val next = state.slices.getOrNull((activeIndex + 1).mod(state.slices.size.coerceAtLeast(1)))
                        next?.let { viewModel.activateSlice(it) }
                    },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.SwapHoriz,
                            contentDescription = "Trocar de atividade",
                            tint = BoardPalette.InkMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.width(20.dp))

                Surface(
                    onClick = {
                        if (service.isTracking) {
                            viewModel.pauseActive()
                        } else {
                            state.slices.firstOrNull()?.let { viewModel.activateSlice(it) }
                        }
                    },
                    shape = CircleShape,
                    color = if (service.isTracking) accent else accent.copy(alpha = 0.18f),
                    modifier = Modifier
                        .size(62.dp)
                        .border(1.5.dp, accent.copy(alpha = if (service.isTracking) 0f else 0.55f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (service.isTracking) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (service.isTracking) "Pausar" else "Iniciar",
                            tint = if (service.isTracking) Color.White else accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
