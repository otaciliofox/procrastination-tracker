package com.foxlab.procrastinationtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxlab.procrastinationtracker.R
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity

/**
 * Renames a single activity (and optionally swaps its icon). Works on Duo/Tri too -- naming the
 * two halves is the whole point of the reference app; only *adding or removing* slices is what
 * forks a built-in profile into a Custom one.
 */
@Composable
fun RenameSliceDialog(
    slice: ActivitySliceEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, iconKey: String?) -> Unit
) {
    var title by remember(slice.id) { mutableStateOf(slice.title) }
    var iconKey by remember(slice.id) { mutableStateOf(slice.iconKey) }
    var showIconPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tracker_rename_activity), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.tracker_slice_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showIconPicker = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ActivityIconRegistry.iconFor(title, iconKey),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.tracker_select_icon),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title.trim(), iconKey) },
                enabled = title.isNotBlank()
            ) {
                Text(stringResource(R.string.tracker_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.tracker_cancel)) }
        }
    )

    if (showIconPicker) {
        IconPickerDialog(
            selectedKey = iconKey,
            onSelect = { iconKey = it },
            onDismiss = { showIconPicker = false }
        )
    }
}
