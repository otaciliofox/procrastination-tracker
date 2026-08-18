package com.foxlab.procrastinationtracker.ui

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.foxlab.procrastinationtracker.service.ScreenLockAccessibilityService
import com.foxlab.procrastinationtracker.trackerdata.LiveSessionState
import com.foxlab.procrastinationtracker.viewmodel.TrackerViewModel

/**
 * Tracker mode: the board *is* the screen -- no toolbar, no bottom bar. Everything else hides
 * behind the "..." button ([TrackerOptionsSheet]), and the report only exists once there's at
 * least a minute recorded.
 */
@Composable
fun TrackerScreen(onGoHome: () -> Unit, viewModel: TrackerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showOptions by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }
    var showAdjust by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingProfileId by remember { mutableStateOf<String?>(null) }
    var renamingSliceId by remember { mutableStateOf<String?>(null) }
    var keepAwake by remember { mutableStateOf(true) }

    // Tracker mode never asks for notifications on its own; this is the opt-in in the menu.
    val notifications = rememberNotificationPermission()

    // Picking up this device while the other one is counting: offered once per arrival, not
    // nagged, and only when this device is idle -- see HandoffDialog for the three answers.
    var handoff by remember { mutableStateOf<LiveSessionState?>(null) }
    var handoffDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(state.service.isTracking, state.slices) {
        handoff = if (handoffDismissed) null else viewModel.remoteRunningSession()
    }

    // The play button resumes what was running before the pause, not blindly the first band.
    var lastSliceId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.service.activeSliceId) {
        state.service.activeSliceId?.let { lastSliceId = it }
    }

    LaunchedEffect(keepAwake) {
        val activity = context as? Activity ?: return@LaunchedEffect
        if (keepAwake) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Spec 002 §4.1: back with a session running opens the menu instead of dropping the session.
    BackHandler {
        if (state.service.isTracking) showOptions = true else onGoHome()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TrackerBoard(
            slices = state.slices,
            activeSliceId = state.service.activeSliceId,
            resumeSliceId = lastSliceId,
            isTracking = state.service.isTracking,
            liveElapsedMillis = state.service.elapsedMillis,
            todayTotals = state.todayTotals,
            todayAllProfilesMillis = state.todayAllProfilesMillis,
            showReportButton = state.hasReportableHistory,
            onSliceTap = { slice ->
                if (slice.id == state.service.activeSliceId) viewModel.pauseActive() else viewModel.activateSlice(slice)
            },
            onSliceLongPress = { slice -> renamingSliceId = slice.id },
            onPlayPauseTap = {
                if (state.service.isTracking) {
                    viewModel.pauseActive()
                } else {
                    val resume = state.slices.firstOrNull { it.id == lastSliceId } ?: state.slices.firstOrNull()
                    resume?.let { viewModel.activateSlice(it) }
                }
            },
            onOptionsTap = { showOptions = true },
            onReportTap = { showReport = true }
        )
    }

    if (showOptions) {
        TrackerOptionsSheet(
            profiles = state.profiles,
            activeProfileId = state.activeProfile?.id,
            slices = state.slices,
            isTracking = state.service.isTracking,
            keepAwake = keepAwake,
            canShowReport = state.hasReportableHistory,
            notificationsEnabled = notifications.isGranted,
            onRequestNotifications = {
                // Android stops showing the system prompt after two refusals, so a second ask
                // has to be a trip to the settings screen instead of a dialog that never appears.
                if (notifications.isSupported && !notifications.isGranted) {
                    notifications.request()
                }
                if (!notifications.isGranted) QuickControls.openNotificationSettings(context)
            },
            canAddTile = QuickControls.canRequestTile(),
            onAddTile = { showOptions = false; QuickControls.requestAddTile(context) },
            onOpenNotificationSettings = {
                showOptions = false
                QuickControls.openNotificationSettings(context)
            },
            onAdjustToday = { showOptions = false; showAdjust = true },
            onKeepAwakeChange = { keepAwake = it },
            onSelectProfile = { viewModel.switchProfile(it) },
            onNewProfile = { showOptions = false; showCreateDialog = true },
            onEditProfile = { showOptions = false; editingProfileId = state.activeProfile?.id },
            onRenameSlice = { slice -> showOptions = false; renamingSliceId = slice.id },
            onShowReport = { showOptions = false; showReport = true },
            onPause = { showOptions = false; viewModel.pauseActive() },
            onStop = { showOptions = false; viewModel.stopActive() },
            onRestart = { showOptions = false; viewModel.discardActive() },
            onLockScreen = {
                showOptions = false
                val locked = ScreenLockAccessibilityService.requestLockScreenIfAvailable()
                if (!locked) {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            },
            onGoHome = { showOptions = false; onGoHome() },
            onExitApp = {
                showOptions = false
                viewModel.stopActive()
                (context as? Activity)?.finish()
            },
            onDismiss = { showOptions = false }
        )
    }

    handoff?.let { remote ->
        HandoffDialog(
            remote = remote,
            onContinueHere = {
                viewModel.takeOverRemoteSession(remote)
                handoff = null
            },
            onStartNew = {
                val slice = state.slices.firstOrNull { it.id == lastSliceId }
                    ?: state.slices.firstOrNull()
                if (slice != null) viewModel.closeRemoteAndStartNew(remote, slice)
                handoff = null
            },
            onDismiss = {
                handoffDismissed = true
                handoff = null
            }
        )
    }

    if (showAdjust) {
        AdjustTodaySheet(
            profileTitle = state.activeProfile?.title.orEmpty(),
            slices = state.slices,
            todayTotals = state.todayTotals,
            onApplyCorrections = { corrections ->
                // Only rewrite what the user actually changed -- the editor works in whole
                // minutes, so anything matching at minute resolution is left untouched.
                corrections.forEach { (sliceId, millis) ->
                    val current = state.todayTotals[sliceId] ?: 0L
                    if (current / 60_000L != millis / 60_000L) viewModel.setTodayTotal(sliceId, millis)
                }
            },
            onResetToday = { state.activeProfile?.let { viewModel.resetTodayForProfile(it.id) } },
            onDismiss = { showAdjust = false }
        )
    }

    if (showReport) {
        TrackerHistoryModal(
            currentWeekSummary = state.currentWeekSummary,
            pastWeeksSummaries = state.pastWeeksSummaries,
            profileBreakdowns = state.profileBreakdowns,
            watchBreakdown = state.watchBreakdown,
            onDismiss = { showReport = false }
        )
    }

    renamingSliceId?.let { sliceId ->
        val slice = state.slices.firstOrNull { it.id == sliceId }
        if (slice == null) {
            renamingSliceId = null
        } else {
            RenameSliceDialog(
                slice = slice,
                onDismiss = { renamingSliceId = null },
                onSave = { title, iconKey ->
                    viewModel.updateSlice(slice.id, title, iconKey)
                    renamingSliceId = null
                }
            )
        }
    }

    if (showCreateDialog) {
        CreateCustomProfileDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, slices ->
                viewModel.createCustomFromScratch(title, slices) { result ->
                    result.fold(
                        onSuccess = { viewModel.switchProfile(it) },
                        onFailure = { showError(context, it) }
                    )
                }
            }
        )
    }

    editingProfileId?.let { profileId ->
        val profile = state.profiles.firstOrNull { it.id == profileId }
        if (profile != null) {
            EditProfileDialog(
                profile = profile,
                slices = state.slices,
                onDismiss = { editingProfileId = null },
                onRenameCustomProfile = { viewModel.renameCustomProfile(profileId, it) },
                onUpdateSlice = { sliceId, title, iconKey -> viewModel.updateSlice(sliceId, title, iconKey) },
                onAddSlice = { title, iconKey -> viewModel.addSliceToCustomProfile(profileId, title, iconKey) { it.onFailure { e -> showError(context, e) } } },
                onRemoveSlice = { sliceId -> viewModel.removeSliceFromCustomProfile(sliceId) { it.onFailure { e -> showError(context, e) } } },
                onDeleteProfile = { viewModel.deleteCustomProfile(profileId) },
                onForkToCustom = { editedSlices ->
                    viewModel.forkToCustom(profileId, editedSlices, null) { result ->
                        result.fold(
                            onSuccess = { viewModel.switchProfile(it) },
                            onFailure = { showError(context, it) }
                        )
                    }
                }
            )
        }
    }
}

private fun showError(context: android.content.Context, error: Throwable) {
    android.widget.Toast.makeText(context, error.message ?: "Não foi possível concluir", android.widget.Toast.LENGTH_SHORT).show()
}
