package com.foxlab.procrastinationtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxlab.procrastinationtracker.R
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.LayoutProfileEntity
import com.foxlab.procrastinationtracker.ui.theme.BoardPalette

/**
 * Everything that used to live in the toolbar and the bottom bar, folded into the "..." button so
 * the board itself can stay full-bleed (spec 002 §4.1's back-menu actions -- Pausar / Parar /
 * Reiniciar / Sair -- are the "Sessão" block here).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerOptionsSheet(
    profiles: List<LayoutProfileEntity>,
    activeProfileId: String?,
    slices: List<ActivitySliceEntity>,
    isTracking: Boolean,
    keepAwake: Boolean,
    canShowReport: Boolean,
    notificationsEnabled: Boolean,
    onRequestNotifications: () -> Unit,
    canAddTile: Boolean,
    onAddTile: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onAdjustToday: () -> Unit,
    onKeepAwakeChange: (Boolean) -> Unit,
    onSelectProfile: (String) -> Unit,
    onNewProfile: () -> Unit,
    onEditProfile: () -> Unit,
    onRenameSlice: (ActivitySliceEntity) -> Unit,
    onShowReport: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onLockScreen: () -> Unit,
    onGoHome: () -> Unit,
    onExitApp: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

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
                .padding(bottom = 12.dp)
        ) {
            SectionLabel(stringResource(R.string.tracker_section_activities))

            slices.forEach { slice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ActivityIconRegistry.iconFor(slice.title, slice.iconKey),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = slice.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRenameSlice(slice) }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.tracker_rename_activity),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.tracker_rename_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            SheetDivider()
            SectionLabel(stringResource(R.string.tracker_section_profile))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                profiles.forEach { profile ->
                    FilterChip(
                        selected = profile.id == activeProfileId,
                        onClick = { onSelectProfile(profile.id) },
                        label = { Text(profile.title) }
                    )
                }
                AssistChip(
                    onClick = onNewProfile,
                    label = { Text(stringResource(R.string.tracker_new_profile_short)) },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            SheetAction(
                icon = Icons.Filled.Tune,
                label = stringResource(R.string.tracker_edit_profile_full),
                onClick = onEditProfile
            )

            SheetDivider()
            if (canShowReport) {
                SheetAction(
                    icon = Icons.Filled.Insights,
                    label = stringResource(R.string.tracker_view_weekly_history),
                    onClick = onShowReport
                )
            }
            SheetAction(
                icon = Icons.Filled.RestartAlt,
                label = stringResource(R.string.tracker_adjust_today),
                onClick = onAdjustToday
            )

            if (isTracking) {
                SheetDivider()
                SectionLabel(stringResource(R.string.tracker_section_session))
                SheetAction(Icons.Filled.Pause, stringResource(R.string.tracker_menu_pause), onPause)
                SheetAction(Icons.Filled.Stop, stringResource(R.string.tracker_menu_stop), onStop)
                SheetAction(
                    icon = Icons.Filled.RestartAlt,
                    label = stringResource(R.string.tracker_menu_restart),
                    onClick = onRestart,
                    tint = MaterialTheme.colorScheme.error
                )
            }

            SheetDivider()
            SectionLabel(stringResource(R.string.tracker_section_screen))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tracker_keep_awake),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = keepAwake, onCheckedChange = onKeepAwakeChange)
            }

            SheetAction(Icons.Filled.Lock, stringResource(R.string.tracker_lock_now), onLockScreen)

            // Opt-in, never asked at launch: tracking already runs in the background without it.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tracker_notification_permission),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = notificationsEnabled,
                    enabled = !notificationsEnabled,
                    onCheckedChange = { if (it) onRequestNotifications() }
                )
            }

            SheetDivider()
            SectionLabel(stringResource(R.string.tracker_section_quick_controls))

            if (canAddTile) {
                SheetAction(
                    icon = Icons.Filled.Dashboard,
                    label = stringResource(R.string.tracker_add_tile),
                    onClick = onAddTile
                )
            }
            SheetAction(
                icon = Icons.Filled.BubbleChart,
                label = stringResource(R.string.tracker_bubble_settings),
                onClick = onOpenNotificationSettings
            )

            SheetDivider()
            SheetAction(Icons.Filled.Home, stringResource(R.string.tracker_go_home), onGoHome)
            SheetAction(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                label = stringResource(R.string.tracker_menu_exit),
                onClick = onExitApp
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}
