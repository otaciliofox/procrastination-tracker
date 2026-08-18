package com.foxlab.procrastinationtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.foxlab.procrastinationtracker.core.theme.BoardTokens

/**
 * One visual identity for the whole phone app: a near-black navy canvas, activities carrying
 * their own color, and type that leans light and roomy. It replaces the old "wedge palette"
 * (built for the pizza chart that no longer exists) and is deliberately **dark only** -- the
 * board's tinted cards are built on the canvas, and a light mode would need a second, different
 * design rather than a recolored one.
 *
 * The values themselves live in `:core` (BoardTokens), shared byte-for-byte with the watch; this
 * object is only the Compose wrapper around them.
 */
object BoardPalette {
    val Canvas = Color(BoardTokens.CANVAS)
    val CanvasTop = Color(BoardTokens.CANVAS_TOP)
    val Surface = Color(BoardTokens.SURFACE)
    val SurfaceVariant = Color(BoardTokens.SURFACE_VARIANT)
    val Ink = Color(BoardTokens.INK)
    val InkMuted = Color(BoardTokens.INK_MUTED)
    val Outline = Color(BoardTokens.OUTLINE)
    val OutlineSoft = Color(BoardTokens.OUTLINE_SOFT)

    val Blue = Color(BoardTokens.BLUE)
    val Teal = Color(BoardTokens.TEAL)
    val Violet = Color(BoardTokens.VIOLET)
    val Amber = Color(BoardTokens.AMBER)
    val Green = Color(BoardTokens.GO_GREEN)

    /** Procrastination (and the break phase) always read warm. */
    val Rose = Color(BoardTokens.ROSE)
}

private val BoardColors = darkColorScheme(
    primary = BoardPalette.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1D3768),
    onPrimaryContainer = Color(0xFFC9DCFF),
    secondary = BoardPalette.Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0E3B39),
    onSecondaryContainer = Color(0xFFB6F1EA),
    tertiary = BoardPalette.Violet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2B1D57),
    onTertiaryContainer = Color(0xFFDDD1FF),
    error = BoardPalette.Rose,
    onError = Color.White,
    errorContainer = Color(0xFF4A1421),
    onErrorContainer = Color(0xFFFFD3DA),
    background = BoardPalette.Canvas,
    onBackground = BoardPalette.Ink,
    surface = BoardPalette.Surface,
    onSurface = BoardPalette.Ink,
    surfaceVariant = BoardPalette.SurfaceVariant,
    onSurfaceVariant = BoardPalette.InkMuted,
    surfaceContainer = BoardPalette.Surface,
    surfaceContainerHigh = BoardPalette.SurfaceVariant,
    outline = BoardPalette.Outline,
    outlineVariant = BoardPalette.OutlineSoft,
    scrim = Color(0xCC05070E)
)

@Composable
fun ProcrastinationTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BoardColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
