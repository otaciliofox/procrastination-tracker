package com.foxlab.procrastinationtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxlab.procrastinationtracker.R
import com.foxlab.procrastinationtracker.core.toHoursMinutesString
import com.foxlab.procrastinationtracker.core.TimerDaySummary
import com.foxlab.procrastinationtracker.ui.theme.BoardPalette

/**
 * The Timer mode's report: how much of today was focus, how much was break, and how many cycles
 * actually closed. Shown live under the clock and again in the Histórico tab, so it is one
 * composable rather than two drifting copies.
 */
@Composable
fun TimerDaySummaryCard(summary: TimerDaySummary, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, BoardPalette.OutlineSoft, shape)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.timer_summary_title),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = BoardPalette.InkMuted
        )
        Spacer(Modifier.height(10.dp))

        if (!summary.hasAnything) {
            Text(
                text = stringResource(R.string.timer_summary_empty),
                fontSize = 13.sp,
                color = BoardPalette.InkMuted
            )
            return@Column
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryValue(
                label = stringResource(R.string.timer_summary_focus),
                value = summary.focusMillis.toHoursMinutesString(),
                color = BoardPalette.Blue,
                modifier = Modifier.weight(1f)
            )
            SummaryValue(
                label = stringResource(R.string.timer_summary_break),
                value = summary.breakMillis.toHoursMinutesString(),
                color = BoardPalette.Teal,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = if (summary.completedFocusBlocks == 1) stringResource(R.string.timer_summary_cycles_one)
            else stringResource(R.string.timer_summary_cycles, summary.completedFocusBlocks),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = BoardPalette.Ink
        )
        val perMode = summary.focusBlocksByMode.entries
            .filter { it.value > 0 }
            .joinToString(" · ") { "${it.value}× ${it.key.label}" }
        if (perMode.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(text = perMode, fontSize = 12.sp, color = BoardPalette.InkMuted)
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Light, color = BoardPalette.Ink)
    }
}
