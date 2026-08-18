package com.foxlab.procrastinationtracker.watch.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text

/** The phone's two mode cards, sized for a round screen: color rail, icon, name. */
@Composable
fun HomeScreen(onOpenTimer: () -> Unit, onOpenTracker: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WatchPalette.CanvasTop, WatchPalette.Canvas)))
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ModeCard(
            icon = Icons.Filled.ViewAgenda,
            title = "Tracker",
            accent = WatchPalette.Blue,
            onClick = onOpenTracker
        )
        Spacer(Modifier.height(10.dp))
        ModeCard(
            icon = Icons.Filled.Timer,
            title = "Timer",
            accent = WatchPalette.Teal,
            onClick = onOpenTimer
        )
    }
}

@Composable
private fun ModeCard(icon: ImageVector, title: String, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.20f), accent.copy(alpha = 0.10f))))
            .border(1.dp, accent.copy(alpha = 0.30f), shape)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(accent)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = WatchPalette.Ink
            )
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showBackground = true, backgroundColor = 0xFF0A1020)
@Composable
fun HomeScreenPreview() {
    ProcrastinationTrackerWatchTheme {
        HomeScreen(onOpenTimer = {}, onOpenTracker = {})
    }
}
