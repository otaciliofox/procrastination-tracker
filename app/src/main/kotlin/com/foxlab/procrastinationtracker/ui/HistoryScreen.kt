package com.foxlab.procrastinationtracker.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.foxlab.procrastinationtracker.core.Phase
import com.foxlab.procrastinationtracker.core.toHoursMinutesString
import com.foxlab.procrastinationtracker.ui.theme.BoardPalette
import com.foxlab.procrastinationtracker.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Timer-mode history, in the same card language as the board. */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BoardPalette.CanvasTop, BoardPalette.Canvas)))
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        TimerDaySummaryCard(summary = state.summary)

        Spacer(Modifier.height(28.dp))
        SectionTitle("HISTÓRICO COMPLETO")
        Spacer(Modifier.height(12.dp))

        if (state.allHistory.isEmpty()) {
            Text(
                text = "Nenhuma sessão registrada ainda.",
                fontSize = 14.sp,
                color = BoardPalette.InkMuted
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.allHistory) { session ->
                val isFocus = session.phase == Phase.FOCUS
                val accent = if (isFocus) BoardPalette.Blue else BoardPalette.Teal
                val shape = RoundedCornerShape(16.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(accent.copy(alpha = 0.10f))
                        .border(1.dp, accent.copy(alpha = 0.20f), shape)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFocus) "Foco · ${session.mode.label}" else "Intervalo · ${session.mode.label}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BoardPalette.Ink
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = buildString {
                                append(timeFormat.format(Date(session.startTimeMillis)))
                                append(" · ")
                                append(if (session.source == "watch") "Relógio" else "Celular")
                                // Blocks cut short still count as time, but never as a cycle.
                                if (!session.completedFully) append(" · interrompido")
                            },
                            fontSize = 12.sp,
                            color = BoardPalette.InkMuted
                        )
                    }
                    Text(
                        text = session.durationMillis.toHoursMinutesString(),
                        fontSize = 15.sp,
                        color = BoardPalette.Ink
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = BoardPalette.InkMuted
    )
}

