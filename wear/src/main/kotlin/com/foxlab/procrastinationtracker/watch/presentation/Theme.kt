package com.foxlab.procrastinationtracker.watch.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import com.foxlab.procrastinationtracker.core.theme.BoardTokens

/**
 * The watch half of the design system. Every value comes from [BoardTokens] in `:core`, the same
 * object the phone's `BoardPalette` wraps -- so a color changes in one file and both apps follow.
 * What is *not* shared is the layout: a round 1.4" screen needs its own composables, which is why
 * only the tokens cross the module boundary.
 */
object WatchPalette {
    val Canvas = Color(BoardTokens.CANVAS)
    val CanvasTop = Color(BoardTokens.CANVAS_TOP)
    val Surface = Color(BoardTokens.SURFACE)
    val Ink = Color(BoardTokens.INK)
    val InkMuted = Color(BoardTokens.INK_MUTED)
    val OutlineSoft = Color(BoardTokens.OUTLINE_SOFT)
    val Blue = Color(BoardTokens.BLUE)
    val Teal = Color(BoardTokens.TEAL)
    val Rose = Color(BoardTokens.ROSE)
    val Green = Color(BoardTokens.GO_GREEN)

    /** Same rule as the phone: procrastination is rose, everything else follows its position. */
    fun activityColor(title: String, index: Int): Color = Color(BoardTokens.activityColor(title, index))
}

private val WatchColors = Colors(
    primary = WatchPalette.Blue,
    primaryVariant = Color(BoardTokens.INDIGO),
    secondary = WatchPalette.Teal,
    secondaryVariant = Color(BoardTokens.TEAL),
    error = WatchPalette.Rose,
    background = WatchPalette.Canvas,
    surface = WatchPalette.Surface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onError = Color.White,
    onBackground = WatchPalette.Ink,
    onSurface = WatchPalette.Ink,
    onSurfaceVariant = WatchPalette.InkMuted
)

@Composable
fun ProcrastinationTrackerWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = WatchColors, content = content)
}
