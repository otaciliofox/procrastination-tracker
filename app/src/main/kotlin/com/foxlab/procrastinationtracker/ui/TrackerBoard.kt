package com.foxlab.procrastinationtracker.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity
import com.foxlab.procrastinationtracker.core.theme.BoardTokens
import com.foxlab.procrastinationtracker.core.toStopwatchString
import com.foxlab.procrastinationtracker.ui.theme.BoardPalette

/** The board's canvas: everything is drawn on this near-black navy, including the system bars. */
val TrackerCanvas = BoardPalette.Canvas

/** The color an activity carries everywhere: board, report, dialogs -- decided in `:core`. */
fun activityColor(title: String, index: Int): Color = Color(BoardTokens.activityColor(title, index))


/**
 * The tracker board: one band per activity, stacked, where **the height of each band is its share
 * of the day so far** -- the screen doubles as a live bar chart of where the time went. The running
 * band lights up in full color, the others stay as quiet tinted cards.
 *
 * Tap a band to track it (tap again to pause), long press to rename. Heights are measured in real
 * dp instead of layout weights so that anything from 2 to 10 activities keeps a usable minimum
 * band, and the controls live in their own bottom bar so nothing ever covers a timer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackerBoard(
    slices: List<ActivitySliceEntity>,
    activeSliceId: String?,
    resumeSliceId: String?,
    isTracking: Boolean,
    liveElapsedMillis: Long,
    todayTotals: Map<String, Long>,
    todayAllProfilesMillis: Long,
    showReportButton: Boolean,
    onSliceTap: (ActivitySliceEntity) -> Unit,
    onSliceLongPress: (ActivitySliceEntity) -> Unit,
    onPlayPauseTap: () -> Unit,
    onOptionsTap: () -> Unit,
    onReportTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (slices.isEmpty()) return

    val millisFor: (ActivitySliceEntity) -> Long = { slice ->
        val base = todayTotals[slice.id] ?: 0L
        if (slice.id == activeSliceId) base + liveElapsedMillis else base
    }
    // Bands are per profile; the headline is the whole day, whichever profile it was tracked on.
    val profileTotal = slices.sumOf(millisFor)
    val dayTotal = todayAllProfilesMillis + if (isTracking) liveElapsedMillis else 0L
    // The play/pause button borrows the color of the band it acts on -- the running one, or the
    // one a tap would resume -- so the controls never introduce a color of their own.
    val accentIndex = slices.indexOfFirst { it.id == activeSliceId }
        .takeIf { it >= 0 }
        ?: slices.indexOfFirst { it.id == resumeSliceId }.takeIf { it >= 0 }
        ?: 0
    val accent = activityColor(slices[accentIndex].title, accentIndex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BoardPalette.CanvasTop, BoardPalette.Canvas)))
    ) {
        BoardHeader(dayTotal = dayTotal, isTracking = isTracking)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            val spacing = 8.dp
            val shares = bandShares(slices.map(millisFor), slices.indexOfFirst { it.id == activeSliceId })
            val heights = bandHeights(maxHeight, spacing, slices.size, shares)

            Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                slices.forEachIndexed { index, slice ->
                    val isActive = slice.id == activeSliceId
                    val color = activityColor(slice.title, index)
                    val millis = millisFor(slice)
                    val height by animateDpAsState(heights[index], tween(450), label = "bandHeight")

                    ActivityBand(
                        title = slice.title,
                        icon = ActivityIconRegistry.iconFor(slice.title, slice.iconKey),
                        millis = millis,
                        percentOfDay = if (profileTotal > 0) ((millis * 100) / profileTotal).toInt() else null,
                        color = color,
                        isActive = isActive,
                        isRunning = isActive && isTracking,
                        height = height,
                        onTap = { onSliceTap(slice) },
                        onLongPress = { onSliceLongPress(slice) }
                    )
                }
            }
        }

        BoardControls(
            isTracking = isTracking,
            accent = accent,
            showReportButton = showReportButton,
            onPlayPauseTap = onPlayPauseTap,
            onOptionsTap = onOptionsTap,
            onReportTap = onReportTap
        )
    }
}

/**
 * Each band keeps a floor of the free space and splits the rest by how much of the day it holds;
 * the running one is guaranteed a visible lead even in its first seconds.
 */
private fun bandShares(millis: List<Long>, activeIndex: Int): List<Float> {
    val total = millis.sum()
    val base = if (total <= 0L) {
        List(millis.size) { 1f / millis.size }
    } else {
        millis.map { it.toFloat() / total.toFloat() }
    }
    if (activeIndex !in base.indices) return base
    val boost = 0.25f
    return base.mapIndexed { index, share ->
        if (index == activeIndex) share * (1f - boost) + boost else share * (1f - boost)
    }
}

private fun bandHeights(available: Dp, spacing: Dp, count: Int, shares: List<Float>): List<Dp> {
    val usable = available - spacing * (count - 1)
    // Floors shrink as activities pile up, but always leave room for the proportional part on top
    // of them -- otherwise 10 slices would degenerate into 10 identical rows.
    val floor = when {
        count <= 2 -> 140.dp
        count <= 4 -> 96.dp
        count <= 6 -> 74.dp
        count <= 8 -> 60.dp
        else -> 50.dp
    }
    if (usable <= floor * count) return List(count) { usable / count }
    val extra = usable - floor * count
    return shares.map { floor + extra * it }
}

@Composable
private fun BoardHeader(dayTotal: Long, isTracking: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "HOJE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Color.White.copy(alpha = 0.45f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = dayTotal.toStopwatchString(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )
        }

        if (isTracking) {
            PulsingDot(color = BoardPalette.Green)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "rastreando",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
        } else {
            Text(
                text = "pausado",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivityBand(
    title: String,
    icon: ImageVector,
    millis: Long,
    percentOfDay: Int?,
    color: Color,
    isActive: Boolean,
    isRunning: Boolean,
    height: Dp,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val background = if (isActive) {
        Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.72f)))
    } else {
        Brush.horizontalGradient(listOf(color.copy(alpha = 0.16f), color.copy(alpha = 0.09f)))
    }
    val ink by animateColorAsState(
        if (isActive) Color.White else Color.White.copy(alpha = 0.92f),
        tween(300),
        label = "ink"
    )
    val borderAlpha by animateFloatAsState(if (isActive) 0f else 0.22f, tween(300), label = "border")
    val expanded = height >= 132.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(background)
            .border(1.dp, color.copy(alpha = borderAlpha), shape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
                onLongClick = onLongPress
            )
    ) {
        // Color rail: the only thing that identifies an activity when the band is at its smallest.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(if (isActive) Color.White.copy(alpha = 0.6f) else color)
        )

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 20.dp, top = 18.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ink.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp,
                        color = ink.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    if (isRunning) PulsingDot(color = ink)
                    percentOfDay?.let { PercentBadge(it, ink) }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = millis.toStopwatchString(),
                    fontSize = if (height >= 200.dp) 54.sp else 40.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1).sp,
                    color = ink
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ink.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f)
                )
                if (isRunning) {
                    PulsingDot(color = ink)
                    Spacer(Modifier.width(10.dp))
                }
                percentOfDay?.let {
                    PercentBadge(it, ink)
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = millis.toStopwatchString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = ink
                )
            }
        }
    }
}

@Composable
private fun PercentBadge(percent: Int, ink: Color) {
    if (percent <= 0) return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ink.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$percent%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ink.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun BoardControls(
    isTracking: Boolean,
    accent: Color,
    showReportButton: Boolean,
    onPlayPauseTap: () -> Unit,
    onOptionsTap: () -> Unit,
    onReportTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GhostButton(Icons.Filled.MoreHoriz, "Opções", onOptionsTap)

        // Same treatment as the bands: filled while running, tinted-and-outlined while idle.
        Surface(
            onClick = onPlayPauseTap,
            shape = CircleShape,
            color = if (isTracking) accent else accent.copy(alpha = 0.18f),
            border = if (isTracking) null else BorderStroke(1.5.dp, accent.copy(alpha = 0.55f)),
            modifier = Modifier.size(66.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isTracking) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isTracking) "Pausar" else "Iniciar",
                    tint = if (isTracking) Color.White else accent,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        if (showReportButton) {
            GhostButton(Icons.Filled.Insights, "Relatório", onReportTap)
        } else {
            Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
private fun GhostButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.10f),
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
