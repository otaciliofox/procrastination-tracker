package com.foxlab.procrastinationtracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.foxlab.procrastinationtracker.R
import com.foxlab.procrastinationtracker.core.TimerMode
import com.foxlab.procrastinationtracker.core.TimerPlan

/** Editor for the user-defined mode: four numbers, clamped by [TimerPlan.sanitized]. */
@Composable
fun CustomPlanDialog(
    plan: TimerPlan,
    onDismiss: () -> Unit,
    onSave: (TimerPlan) -> Unit
) {
    var focus by remember { mutableStateOf(plan.focusMinutes.toString()) }
    var shortBreak by remember { mutableStateOf(plan.shortBreakMinutes.toString()) }
    var longBreak by remember { mutableStateOf(plan.longBreakMinutes.toString()) }
    var cycles by remember {
        mutableStateOf(
            if (plan.hasLongBreak) plan.focusSessionsBeforeLongBreak.toString()
            else TimerPlan.of(TimerMode.CUSTOM).focusSessionsBeforeLongBreak.toString()
        )
    }

    val isValid = listOf(focus, shortBreak, longBreak, cycles).all { it.toIntOrNull() != null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.timer_custom_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                MinutesField(focus, { focus = it }, stringResource(R.string.timer_custom_focus))
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    MinutesField(
                        shortBreak,
                        { shortBreak = it },
                        stringResource(R.string.timer_custom_short_break),
                        Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    MinutesField(
                        longBreak,
                        { longBreak = it },
                        stringResource(R.string.timer_custom_long_break),
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                MinutesField(cycles, { cycles = it }, stringResource(R.string.timer_custom_cycles))
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onSave(
                        TimerPlan(
                            mode = TimerMode.CUSTOM,
                            focusMinutes = focus.toInt(),
                            shortBreakMinutes = shortBreak.toInt(),
                            longBreakMinutes = longBreak.toInt(),
                            focusSessionsBeforeLongBreak = cycles.toInt()
                        ).sanitized()
                    )
                }
            ) { Text(stringResource(R.string.tracker_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.tracker_cancel)) }
        }
    )
}

@Composable
private fun MinutesField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter { it.isDigit() }.take(3)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth()
    )
}
