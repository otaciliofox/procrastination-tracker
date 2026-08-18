package com.foxlab.procrastinationtracker.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Notifications are never requested at launch. Tracker mode doesn't need them at all (it counts
 * in the background either way), and the pre-configured modes only need them at the moment the
 * user starts a block that has to alert -- so the prompt arrives with a reason attached.
 */
class NotificationPermissionState(
    val isGranted: Boolean,
    val isSupported: Boolean,
    private val requestPermission: () -> Unit
) {
    /** Runs [onReady] straight away when nothing is needed, otherwise asks first. */
    fun ensure(onReady: () -> Unit) {
        if (!isSupported || isGranted) onReady() else requestPermission()
    }

    fun request() = requestPermission()
}

fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun rememberNotificationPermission(onResult: (granted: Boolean) -> Unit = {}): NotificationPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasNotificationPermission(context)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted = result || hasNotificationPermission(context)
        onResult(granted)
    }

    return NotificationPermissionState(
        isGranted = granted,
        isSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
        requestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onResult(true)
            }
        }
    )
}
