package com.foxlab.procrastinationtracker.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.foxlab.procrastinationtracker.ProcrastinationTrackerApp
import com.foxlab.procrastinationtracker.R
import com.foxlab.procrastinationtracker.core.toStopwatchString
import com.foxlab.procrastinationtracker.ui.BubbleActivity

/**
 * The floating bubble: the app's controls hovering over whatever you're actually doing.
 *
 * Android only bubbles *conversation* notifications, so this posts its own notification -- with a
 * long-lived shortcut, a Person and MessagingStyle -- separate from the ongoing control
 * notification, which keeps its normal (and more useful) layout in the shade. If the user hasn't
 * allowed bubbles for the app, the system quietly shows it as a regular notification instead,
 * which is why it is silent and low priority.
 */
object BubbleController {

    private const val SHORTCUT_ID = "tracker-bubble"
    private const val NOTIFICATION_ID = 45

    fun show(context: Context, activityTitle: String, elapsedMillis: Long) {
        ensureShortcut(context, activityTitle)

        val person = Person.Builder()
            .setName(activityTitle)
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_notification))
            .setKey(SHORTCUT_ID)
            .setBot(true)
            .build()

        val bubbleIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, BubbleActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bubble = NotificationCompat.BubbleMetadata.Builder(
            bubbleIntent,
            IconCompat.createWithResource(context, R.drawable.ic_notification)
        )
            .setDesiredHeight(320)
            // Not auto-expanded: it should be there when wanted, not jump in front of what the
            // user opened the other app to do.
            .setAutoExpandBubble(false)
            .setSuppressNotification(true)
            .build()

        val notification = NotificationCompat.Builder(context, ProcrastinationTrackerApp.BUBBLE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setShortcutId(SHORTCUT_ID)
            .setBubbleMetadata(bubble)
            .setSilent(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setStyle(
                NotificationCompat.MessagingStyle(person)
                    .addMessage(elapsedMillis.toStopwatchString(), System.currentTimeMillis(), person)
            )
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    fun hide(context: Context) {
        runCatching { NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID) }
    }

    /**
     * Bubbles require a long-lived shortcut to hang off. Refreshed with the activity name so the
     * bubble is labelled with what is actually being tracked.
     */
    private fun ensureShortcut(context: Context, activityTitle: String) {
        val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
            .setLongLived(true)
            .setShortLabel(activityTitle.ifBlank { context.getString(R.string.app_name) })
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_notification))
            .setIntent(Intent(context, BubbleActivity::class.java).setAction(Intent.ACTION_VIEW))
            .setPerson(
                Person.Builder()
                    .setName(activityTitle.ifBlank { context.getString(R.string.app_name) })
                    .setKey(SHORTCUT_ID)
                    .setBot(true)
                    .build()
            )
            .build()
        runCatching { ShortcutManagerCompat.pushDynamicShortcut(context, shortcut) }
    }
}
