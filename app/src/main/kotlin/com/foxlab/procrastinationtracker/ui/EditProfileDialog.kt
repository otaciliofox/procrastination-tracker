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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.LayoutProfileEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.ProfileType

data class EditableSliceItem(
    val id: String,
    var title: String,
    var iconKey: String?
)

/**
 * Editing a profile. Duo/Tri (read-only templates) can only have their slices
 * edited locally -- confirming always forks a new Custom profile.
 * Custom profiles are edited in place: rename, change icons, add/remove slices (up to 10), or delete.
 */
@Composable
fun EditProfileDialog(
    profile: LayoutProfileEntity,
    slices: List<ActivitySliceEntity>,
    onDismiss: () -> Unit,
    onRenameCustomProfile: (newTitle: String) -> Unit,
    onUpdateSlice: (sliceId: String, newTitle: String, newIconKey: String?) -> Unit,
    onAddSlice: (title: String, iconKey: String?) -> Unit,
    onRemoveSlice: (sliceId: String) -> Unit,
    onDeleteProfile: () -> Unit,
    onForkToCustom: (editedSlices: List<SliceDraft>) -> Unit
) {
    val isCustom = profile.type == ProfileType.CUSTOM
    var profileTitle by remember(profile.id) { mutableStateOf(profile.title) }
    val sliceEdits = remember(profile.id, slices) {
        mutableStateListOf(*slices.map { EditableSliceItem(it.id, it.title, it.iconKey) }.toTypedArray())
    }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickingIconForIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tracker_edit_profile) + ": " + profile.title) },
        text = {
            Column {
                if (!isCustom) {
                    Text(stringResource(R.string.tracker_readonly_notice), style = MaterialTheme.typography.bodySmall)
                } else {
                    OutlinedTextField(
                        value = profileTitle,
                        onValueChange = { profileTitle = it },
                        label = { Text(stringResource(R.string.tracker_profile_name)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }

                LazyColumn(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                    itemsIndexed(sliceEdits) { index, item ->
                        val icon = ActivityIconRegistry.iconFor(item.title, item.iconKey)
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
                                value = item.title,
                                onValueChange = { sliceEdits[index] = item.copy(title = it) },
                                label = { Text("${stringResource(R.string.tracker_slice_name)} ${index + 1}") },
                                modifier = Modifier.weight(1f)
                            )
                            if (isCustom && sliceEdits.size > MIN_SLICES) {
                                IconButton(onClick = {
                                    onRemoveSlice(item.id)
                                    sliceEdits.removeAt(index)
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.tracker_remove_slice))
                                }
                            }
                        }
                    }
                }

                if (isCustom && sliceEdits.size < MAX_SLICES) {
                    TextButton(onClick = { onAddSlice("Nova fatia", null) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.tracker_add_slice))
                    }
                }

                if (isCustom) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.tracker_delete_profile))
                    }
                }

                if (confirmDelete) {
                    AlertDialog(
                        onDismissRequest = { confirmDelete = false },
                        title = { Text(stringResource(R.string.tracker_delete_profile)) },
                        text = { Text(profile.title) },
                        confirmButton = {
                            TextButton(onClick = { onDeleteProfile(); confirmDelete = false; onDismiss() }) {
                                Text(stringResource(R.string.tracker_delete_profile))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.tracker_cancel)) }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (isCustom) {
                    if (profileTitle.isNotBlank() && profileTitle != profile.title) onRenameCustomProfile(profileTitle.trim())
                    sliceEdits.forEach { item ->
                        val original = slices.firstOrNull { it.id == item.id }
                        if (item.title.isNotBlank() && (item.title != original?.title || item.iconKey != original?.iconKey)) {
                            onUpdateSlice(item.id, item.title.trim(), item.iconKey)
                        }
                    }
                    onDismiss()
                } else {
                    onForkToCustom(sliceEdits.map { SliceDraft(it.title.trim(), it.iconKey) })
                    onDismiss()
                }
            }) {
                Text(if (isCustom) stringResource(R.string.tracker_save) else stringResource(R.string.tracker_save_as_custom))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.tracker_cancel)) }
        }
    )

    pickingIconForIndex?.let { index ->
        IconPickerDialog(
            selectedKey = sliceEdits.getOrNull(index)?.iconKey,
            onSelect = { key ->
                sliceEdits[index] = sliceEdits[index].copy(iconKey = key)
                pickingIconForIndex = null
            },
            onDismiss = { pickingIconForIndex = null }
        )
    }
}
