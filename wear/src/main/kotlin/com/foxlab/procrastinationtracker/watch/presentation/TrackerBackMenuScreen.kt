package com.foxlab.procrastinationtracker.watch.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items

/** Same 4 options as the phone's session menu (spec 002 §4.1/§6), in the board's language. */
@Composable
fun TrackerBackMenuScreen(
    onPause: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val options = listOf(
        Triple("Pausar", WatchPalette.Blue, onPause),
        Triple("Parar", WatchPalette.Teal, onStop),
        Triple("Reiniciar", WatchPalette.Rose, onRestart),
        Triple("Sair do app", WatchPalette.Rose, onExitApp)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(WatchPalette.CanvasTop, WatchPalette.Canvas)))
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { WatchScreenTitle("Sessão") }
            items(options) { (label, accent, action) ->
                WatchListRow(title = label, accent = accent, onClick = action)
            }
            // Renaming activities, correcting the day and the weekly report are phone-side: typing
            // and fine adjustments on a 1.4" screen would be worse than the walk to the phone.
            item {
                WatchListRow(
                    title = "Editar no celular",
                    accent = WatchPalette.Blue,
                    onClick = { OpenOnPhone.launch(context) }
                )
            }
        }
    }
}
