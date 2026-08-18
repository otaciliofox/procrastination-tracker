package com.foxlab.procrastinationtracker.service

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.foxlab.procrastinationtracker.ProcrastinationTrackerApp
import com.foxlab.procrastinationtracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile: pause or resume tracking from the notification shade, without opening the
 * app or leaving whatever you're doing.
 *
 * This exists because the floating bubble is not universally available -- Samsung's One UI
 * refuses to bubble notifications and offers its own pop-up instead -- while a tile works the
 * same everywhere and is two taps from any screen. The two features overlap on purpose: whichever
 * one a given phone supports, the user still gets "control it without opening the app".
 */
class TrackerTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        val state = TrackerForegroundService.uiState.value
        if (state.isTracking) {
            sendAction(TrackerForegroundService.ACTION_PAUSE)
            render(forceIdle = true)
            return
        }

        // Not tracking: resume the last activity, or the profile's first one on a cold start.
        scope.launch {
            val repository = (application as ProcrastinationTrackerApp).trackerRepository
            val slice = repository.getNextSlice(null) ?: return@launch
            sendResume(slice.id, slice.title)
        }
    }

    private fun sendAction(action: String) {
        val intent = android.content.Intent(this, TrackerForegroundService::class.java).setAction(action)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun sendResume(sliceId: String, title: String) {
        val intent = android.content.Intent(this, TrackerForegroundService::class.java)
            .setAction(TrackerForegroundService.ACTION_ACTIVATE_SLICE)
            .putExtra(TrackerForegroundService.EXTRA_SLICE_ID, sliceId)
            .putExtra(TrackerForegroundService.EXTRA_SLICE_TITLE, title)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun render(forceIdle: Boolean = false) {
        val tile = qsTile ?: return
        val state = TrackerForegroundService.uiState.value
        val tracking = state.isTracking && !forceIdle

        tile.state = if (tracking) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_notification)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (tracking) {
                state.activeSliceTitle.orEmpty()
            } else {
                getString(R.string.tile_paused)
            }
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Asks the system to refresh the tile after the tracking state changes. */
        fun refresh(context: android.content.Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
            runCatching {
                requestListeningState(context, ComponentName(context, TrackerTileService::class.java))
            }
        }
    }
}
