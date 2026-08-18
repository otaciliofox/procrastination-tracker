package com.foxlab.procrastinationtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxlab.procrastinationtracker.R
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity
import com.foxlab.procrastinationtracker.ui.theme.BoardPalette

/**
 * "Esqueci de trocar de atividade" recovery, in two flavours:
 *
 *  - **Corrigir tempo**: type the real hours/minutes for each activity of the current profile.
 *  - **Reiniciar contagem**: throw today away *for this profile only* -- the other profiles keep
 *    their time and the day headline recomputes without the cleared one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustTodaySheet(
    profileTitle: String,
    slices: List<ActivitySliceEntity>,
    todayTotals: Map<String, Long>,
    onApplyCorrections: (Map<String, Long>) -> Unit,
    onResetToday: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableStateOf(AdjustStep.MENU) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BoardPalette.CanvasTop
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.tracker_adjust_today),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = profileTitle,
                fontSize = 13.sp,
                color = BoardPalette.InkMuted
            )
            Spacer(Modifier.height(18.dp))

            when (step) {
                AdjustStep.MENU -> {
                    OptionCard(
                        icon = Icons.Filled.Edit,
                        accent = BoardPalette.Blue,
                        title = stringResource(R.string.tracker_adjust_fix_title),
                        subtitle = stringResource(R.string.tracker_adjust_fix_body),
                        onClick = { step = AdjustStep.EDIT }
                    )
                    Spacer(Modifier.height(12.dp))
                    OptionCard(
                        icon = Icons.Filled.RestartAlt,
                        accent = BoardPalette.Rose,
                        title = stringResource(R.string.tracker_adjust_reset_title),
                        subtitle = stringResource(R.string.tracker_adjust_reset_body),
                        onClick = { step = AdjustStep.CONFIRM_RESET }
                    )
                }

                AdjustStep.EDIT -> EditTotals(
                    slices = slices,
                    todayTotals = todayTotals,
                    onCancel = { step = AdjustStep.MENU },
                    onApply = { onApplyCorrections(it); onDismiss() }
                )

                AdjustStep.CONFIRM_RESET -> {
                    Text(
                        text = stringResource(R.string.tracker_adjust_reset_confirm),
                        fontSize = 14.sp,
                        color = BoardPalette.Ink
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { step = AdjustStep.MENU }) {
                            Text(stringResource(R.string.tracker_cancel))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onResetToday(); onDismiss() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BoardPalette.Rose,
                                contentColor = Color.White
                            )
                        ) {
                            Text(stringResource(R.string.tracker_adjust_reset_confirm_action))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private enum class AdjustStep { MENU, EDIT, CONFIRM_RESET }

@Composable
private fun EditTotals(
    slices: List<ActivitySliceEntity>,
    todayTotals: Map<String, Long>,
    onCancel: () -> Unit,
    onApply: (Map<String, Long>) -> Unit
) {
    // One hour/minute pair per activity, seeded with what's recorded today.
    val hours = remember(slices) {
        mutableStateMapOf<String, String>().apply {
            slices.forEach { put(it.id, ((todayTotals[it.id] ?: 0L) / 3_600_000L).toString()) }
        }
    }
    val minutes = remember(slices) {
        mutableStateMapOf<String, String>().apply {
            slices.forEach { put(it.id, (((todayTotals[it.id] ?: 0L) % 3_600_000L) / 60_000L).toString()) }
        }
    }

    Text(
        text = stringResource(R.string.tracker_adjust_fix_hint),
        fontSize = 13.sp,
        color = BoardPalette.InkMuted
    )
    Spacer(Modifier.height(14.dp))

    slices.forEachIndexed { index, slice ->
        val color = activityColor(slice.title, index)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = slice.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = BoardPalette.Ink,
                modifier = Modifier.weight(1f)
            )
            NumberBox(
                value = hours[slice.id].orEmpty(),
                onValueChange = { hours[slice.id] = it },
                suffix = stringResource(R.string.tracker_adjust_hours),
                max = 23
            )
            Spacer(Modifier.width(8.dp))
            NumberBox(
                value = minutes[slice.id].orEmpty(),
                onValueChange = { minutes[slice.id] = it },
                suffix = stringResource(R.string.tracker_adjust_minutes),
                max = 59
            )
        }
    }

    Spacer(Modifier.height(18.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onCancel) { Text(stringResource(R.string.tracker_cancel)) }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = {
                val corrections = slices.associate { slice ->
                    val h = hours[slice.id]?.toLongOrNull() ?: 0L
                    val m = minutes[slice.id]?.toLongOrNull() ?: 0L
                    slice.id to (h * 3_600_000L + m * 60_000L)
                }
                onApply(corrections)
            }
        ) { Text(stringResource(R.string.tracker_save)) }
    }
}

/** Digits only, and never a value a single day can't hold (23h / 59min). */
@Composable
private fun NumberBox(value: String, onValueChange: (String) -> Unit, suffix: String, max: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = { new ->
            val digits = new.filter { it.isDigit() }.take(2)
            val clamped = digits.toIntOrNull()?.coerceAtMost(max)?.toString() ?: digits
            onValueChange(clamped)
        },
        singleLine = true,
        suffix = { Text(suffix, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(86.dp)
    )
}

@Composable
private fun OptionCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(accent.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BoardPalette.Ink)
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 12.sp, color = BoardPalette.InkMuted)
        }
    }
}
