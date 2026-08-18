package com.foxlab.procrastinationtracker.watch.presentation

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.foxlab.procrastinationtracker.trackerdata.TrackerRepository
import com.foxlab.procrastinationtracker.trackerdata.CompanionPresence
import com.foxlab.procrastinationtracker.trackerdata.LiveSessionState
import com.foxlab.procrastinationtracker.trackerdata.LiveSessionSync
import com.foxlab.procrastinationtracker.trackerdata.RemoteLiveSession
import com.foxlab.procrastinationtracker.trackerdata.dao.SliceTotal
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.LayoutProfileEntity
import com.foxlab.procrastinationtracker.watch.WatchApplication
import com.foxlab.procrastinationtracker.watch.service.ActivitySyncSender
import com.foxlab.procrastinationtracker.watch.service.TrackerForegroundService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val repository: TrackerRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    data class UiState(
        val profiles: List<LayoutProfileEntity> = emptyList(),
        val activeProfile: LayoutProfileEntity? = null,
        val slices: List<ActivitySliceEntity> = emptyList(),
        val todayTotals: Map<String, Long> = emptyMap(),
        /** Activity count per profile, shown as information in the picker. */
        val activityCountByProfile: Map<String, Int> = emptyMap(),
        /** Today per device: "watch" -> (sliceId -> millis), "phone" -> ... */
        val todayByDevice: Map<String, Map<String, Long>> = emptyMap(),
        val service: TrackerForegroundService.UiState = TrackerForegroundService.UiState()
    ) {
        fun liveTodayTotal(sliceId: String): Long {
            val base = todayTotals[sliceId] ?: 0L
            return if (service.activeSliceId == sliceId) base + service.elapsedMillis else base
        }
    }

    private val activeProfileFlow = repository.observeActiveProfile()

    private data class SlicesAndTotals(val slices: List<ActivitySliceEntity>, val today: List<SliceTotal>)

    private val slicesAndTotalsFlow = activeProfileFlow.flatMapLatest { profile ->
        if (profile == null) {
            flowOf(SlicesAndTotals(emptyList(), emptyList()))
        } else {
            combine(repository.observeSlices(profile.id), repository.observeTodayTotals(profile.id)) { slices, today ->
                SlicesAndTotals(slices, today)
            }
        }
    }

    private val extrasFlow = combine(
        repository.observeAllActiveSlices(),
        repository.observeTodayTotalsByDevice()
    ) { allSlices, byDevice -> allSlices to byDevice }

    val uiState = combine(
        repository.observeProfiles(),
        activeProfileFlow,
        slicesAndTotalsFlow,
        TrackerForegroundService.uiState,
        extrasFlow
    ) { profiles, active, slicesAndTotals, service, extras ->
        val (allSlices, byDevice) = extras
        UiState(
            profiles = profiles,
            activeProfile = active,
            slices = slicesAndTotals.slices,
            todayTotals = slicesAndTotals.today.associate { it.sliceId to it.totalMillis },
            activityCountByProfile = allSlices.groupingBy { it.profileId }.eachCount(),
            todayByDevice = byDevice,
            service = service
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun activateSlice(slice: ActivitySliceEntity) {
        val intent = Intent(context, TrackerForegroundService::class.java).apply {
            action = TrackerForegroundService.ACTION_ACTIVATE_SLICE
            putExtra(TrackerForegroundService.EXTRA_SLICE_ID, slice.id)
            putExtra(TrackerForegroundService.EXTRA_SLICE_TITLE, slice.title)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun pauseActive() = sendAction(TrackerForegroundService.ACTION_PAUSE)
    fun stopActive() = sendAction(TrackerForegroundService.ACTION_STOP)
    fun discardActive() = sendAction(TrackerForegroundService.ACTION_DISCARD)

    /** The phone's running block, if it is still fresh and this watch isn't tracking. */
    suspend fun remoteRunningSession(): LiveSessionState? {
        if (uiState.value.service.isTracking) return null
        // Never heard a "hi" from a companion: nothing to ask, and no reason to pay for a Data
        // Layer lookup that has nobody to answer it.
        if (!CompanionPresence.hasCompanion(context)) return null
        val myId = LiveSessionSync.deviceId(context)
        RemoteLiveSession.runningOnOtherDevice(myId)?.let { return it }
        // Nothing cached (fresh process): ask the Data Layer for the current broadcast.
        val fetched = withContext(Dispatchers.IO) { LiveSessionSync.readRemote(context) }
        if (fetched != null) RemoteLiveSession.update(fetched)
        return fetched?.takeIf { it.isFresh() && it.deviceId != myId }
    }

    /** "Continuar aqui": same block, same start time, now owned by the watch. */
    fun takeOverRemoteSession(remote: LiveSessionState) {
        val intent = Intent(context, TrackerForegroundService::class.java).apply {
            action = TrackerForegroundService.ACTION_TAKE_OVER
            putExtra(TrackerForegroundService.EXTRA_TAKEOVER_SESSION_ID, remote.sessionId)
            putExtra(TrackerForegroundService.EXTRA_SLICE_ID, remote.sliceId)
            putExtra(TrackerForegroundService.EXTRA_SLICE_TITLE, remote.sliceTitle)
            putExtra(TrackerForegroundService.EXTRA_TAKEOVER_STARTED_AT, remote.startedAtMillis)
        }
        ContextCompat.startForegroundService(context, intent)
        RemoteLiveSession.clear()
    }

    /** "Começar um novo": the phone's block is closed and saved, then this watch starts fresh. */
    fun closeRemoteAndStartNew(remote: LiveSessionState, slice: ActivitySliceEntity) {
        viewModelScope.launch {
            // "Começar um novo": the phone closes and saves its own block when it sees this
            // watch take over, so nothing is written on its behalf here.
            activateSlice(slice)
            runCatching { ActivitySyncSender.push(context, repository) }
        }
        RemoteLiveSession.clear()
    }

    fun switchProfile(profileId: String) {
        val intent = Intent(context, TrackerForegroundService::class.java).apply {
            action = TrackerForegroundService.ACTION_SWITCH_PROFILE
            putExtra(TrackerForegroundService.EXTRA_PROFILE_ID, profileId)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun sendAction(action: String) {
        val intent = Intent(context, TrackerForegroundService::class.java).apply { this.action = action }
        ContextCompat.startForegroundService(context, intent)
    }
}
