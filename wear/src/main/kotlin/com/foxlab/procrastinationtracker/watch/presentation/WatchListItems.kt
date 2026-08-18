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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text

/**
 * The watch's own list row, in the board's language: color rail on the left, tinted card, filled
 * when it is the selected one. Wear's stock `Chip` was the last thing still looking like a
 * different app, and the secondary screens are where the user lands right after the board.
 */
@Composable
fun WatchListRow(
    title: String,
    subtitle: String? = null,
    accent: Color = WatchPalette.Blue,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (subtitle == null) 44.dp else 54.dp)
            .clip(shape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.75f)))
                else Brush.horizontalGradient(listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.09f)))
            )
            .border(1.dp, if (selected) Color.Transparent else accent.copy(alpha = 0.25f), shape)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(if (selected) Color.White.copy(alpha = 0.6f) else accent)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 12.dp)
                .height(if (subtitle == null) 44.dp else 54.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else WatchPalette.Ink,
                maxLines = 1
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = if (selected) Color.White.copy(alpha = 0.85f) else WatchPalette.InkMuted,
                    maxLines = 1
                )
            }
        }
    }
}

/** Small centered heading for the secondary screens. */
@Composable
fun WatchScreenTitle(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = WatchPalette.InkMuted
        )
    }
}
