package com.foxlab.procrastinationtracker.watch.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper

/**
 * Hands the task over to the phone. The watch deliberately doesn't try to be the whole app:
 * it keeps 30 days of history and no text editing, so "ver mais" and "editar" are a nudge to the
 * device where those are actually pleasant. If the phone isn't reachable the call simply fails,
 * which is why the caller shows a short confirmation either way.
 */
object OpenOnPhone {

    private const val DEEP_LINK = "procrastinationtracker://open"

    fun launch(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(DEEP_LINK))
        RemoteActivityHelper(context).startRemoteActivity(intent)
    }
}
