package com.foxlab.procrastinationtracker.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxlab.procrastinationtracker.R
import com.foxlab.procrastinationtracker.ui.theme.BoardPalette

/**
 * Same language as the tracker board: the canvas, tinted cards with a color rail, light type.
 * Each mode is one full-width card -- a big target, and a preview of what the mode looks like
 * once you're inside it.
 */
@Composable
fun HomeScreen(onOpenTimer: () -> Unit, onOpenTracker: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BoardPalette.CanvasTop, BoardPalette.Canvas)))
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(top = 40.dp, bottom = 28.dp)) {
            Text(
                text = stringResource(R.string.home_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = BoardPalette.Ink
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_subtitle),
                fontSize = 14.sp,
                color = BoardPalette.InkMuted
            )
        }

        ModeCard(
            icon = Icons.Filled.ViewAgenda,
            title = stringResource(R.string.home_mode_tracker_title),
            subtitle = stringResource(R.string.home_mode_tracker_subtitle),
            accent = BoardPalette.Blue,
            onClick = onOpenTracker,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.height(16.dp))

        ModeCard(
            icon = Icons.Filled.Timer,
            title = stringResource(R.string.home_mode_timer_title),
            subtitle = stringResource(R.string.home_mode_timer_subtitle),
            accent = BoardPalette.Teal,
            onClick = onOpenTimer,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.08f))))
            .border(1.dp, accent.copy(alpha = 0.28f), shape)
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
                .padding(start = 26.dp, end = 22.dp, top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = BoardPalette.Ink
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = BoardPalette.InkMuted
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_open),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = accent
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
