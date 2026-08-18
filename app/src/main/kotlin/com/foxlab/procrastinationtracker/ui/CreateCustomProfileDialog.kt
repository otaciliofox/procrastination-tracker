package com.foxlab.procrastinationtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.foxlab.procrastinationtracker.R
import com.foxlab.procrastinationtracker.trackerdata.MAX_SLICES
import com.foxlab.procrastinationtracker.trackerdata.MIN_SLICES
import com.foxlab.procrastinationtracker.trackerdata.SliceDraft

/** Creating a brand new Custom profile from scratch — 2 to 10 free-form slices with icons. */
@Composable
fun CreateCustomProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, slices: List<SliceDraft>) -> Unit
) {
    var profileTitle by remember { mutableStateOf("") }
    // A profile is always at least two activities, and the pair everyone starts from is
    // focus vs. procrastination. Pre-filling them turns "what am I supposed to type here?"
    // into "edit these two and add more if you want" -- no explaining needed.
    val slices = remember {
        mutableStateListOf(
            SliceDraft("Foco", "focus"),
            SliceDraft("Procrastinando", "coffee")
        )
    }
    var pickingIconForIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tracker_new_profile_from_scratch)) },
        text = {
            Column {
                OutlinedTextField(
                    value = profileTitle,
                    onValueChange = { profileTitle = it },
                    label = { Text(stringResource(R.string.tracker_new_profile_name_hint)) },
                    placeholder = { Text(stringResource(R.string.tracker_new_profile_name_example)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                    items(slices.size) { index ->
                        val draft = slices[index]
                        val icon = ActivityIconRegistry.iconFor(draft.title, draft.iconKey)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    .clickable { pickingIconForIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = stringResource(R.string.tracker_select_icon),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = draft.title,
                                onValueChange = { slices[index] = draft.copy(title = it) },
                                label = { Text("${stringResource(R.string.tracker_slice_name)} ${index + 1}") },
                                modifier = Modifier.weight(1f)
                            )
                            if (slices.size > MIN_SLICES) {
                                IconButton(onClick = { slices.removeAt(index) }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.tracker_remove_slice))
                                }
                            }
                        }
                    }
                }
                if (slices.size < MAX_SLICES) {
                    TextButton(onClick = { slices.add(SliceDraft("", null)) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.tracker_add_slice))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validSlices = slices.filter { it.title.isNotBlank() }
                        .map { it.copy(title = it.title.trim()) }
                    if (profileTitle.isNotBlank() && validSlices.size in MIN_SLICES..MAX_SLICES) {
                        onCreate(profileTitle.trim(), validSlices)
                        onDismiss()
                    }
                }
            ) { Text(stringResource(R.string.tracker_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.tracker_cancel)) }
        }
    )

    pickingIconForIndex?.let { index ->
        IconPickerDialog(
            selectedKey = slices.getOrNull(index)?.iconKey,
            onSelect = { key ->
                slices[index] = slices[index].copy(iconKey = key)
                pickingIconForIndex = null
            },
            onDismiss = { pickingIconForIndex = null }
        )
    }
}
