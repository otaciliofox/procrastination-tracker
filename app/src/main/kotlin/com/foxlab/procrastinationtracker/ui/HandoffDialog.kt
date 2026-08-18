package com.foxlab.procrastinationtracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxlab.procrastinationtracker.core.toStopwatchString
import com.foxlab.procrastinationtracker.trackerdata.LiveSessionState

/**
 * Shown when you pick up this device and the other one is still counting.
 *
 * Three answers, because all three are things people actually mean:
 *  - **Continuar aqui**: the same block changes owner, keeping its start time. Nothing is counted
 *    twice, because the other device drops its open row when it sees the hand-off.
 *  - **Começar um novo**: the other device closes and *saves* its block (that time was real, the
 *    run happened) and this device starts fresh.
 *  - **Deixar como está**: you only opened the app to look; the other device keeps counting.
 */
@Composable
fun HandoffDialog(
    remote: LiveSessionState,
    onContinueHere: () -> Unit,
    onStartNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val deviceLabel = if (remote.deviceKind == "watch") "O relógio" else "O celular"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "$deviceLabel está rastreando",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = "${remote.sliceTitle} · ${remote.elapsedMillis().toStopwatchString()}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Continuar traz esse tempo para cá sem interromper. Começar um novo " +
                        "encerra e guarda o tempo do outro aparelho.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onContinueHere) { Text("Continuar aqui") }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onStartNew) { Text("Começar um novo") }
                TextButton(onClick = onDismiss) { Text("Deixar como está") }
            }
        }
    )
}
