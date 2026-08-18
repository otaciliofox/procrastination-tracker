package com.foxlab.procrastinationtracker.ui

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import com.foxlab.procrastinationtracker.R
import com.foxlab.procrastinationtracker.service.TrackerTileService

/**
 * The two "control it without opening the app" surfaces, and how to offer them.
 *
 * Both are opt-in and both are asked for only when the user reaches for them -- same rule as the
 * notification permission. Neither needs a new manifest permission: the tile is a system dialog
 * the user accepts, and the bubble is a notification, so it rides on the notification permission
 * plus a system-level toggle no app is allowed to flip on its own.
 */
object QuickControls {

    /** Android 13+ can ask the user to add the tile with a system dialog; older versions can't. */
    fun canRequestTile(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun requestAddTile(context: Context) {
        if (!canRequestTile()) return
        val statusBar = context.getSystemService(StatusBarManager::class.java) ?: return
        runCatching {
            statusBar.requestAddTileService(
                ComponentName(context, TrackerTileService::class.java),
                context.getString(R.string.tile_label),
                Icon.createWithResource(context, R.drawable.ic_notification),
                { it.run() },
                { }
            )
        }
    }

    /**
     * Bubbles can only be enabled by the user, in the system settings for this app. Asking again
     * therefore means taking them there -- there is no runtime permission to re-request.
     */
    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.fromParts("package", context.packageName, null))
        }
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
