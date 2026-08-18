package com.foxlab.procrastinationtracker.viewmodel

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.foxlab.procrastinationtracker.ProcrastinationTrackerApp
import com.foxlab.procrastinationtracker.core.ActivityRules
import com.foxlab.procrastinationtracker.service.ActivitySyncSender
import com.foxlab.procrastinationtracker.service.TrackerForegroundService
import com.foxlab.procrastinationtracker.trackerdata.CompanionPresence
import com.foxlab.procrastinationtracker.trackerdata.DeviceBreakdown
import com.foxlab.procrastinationtracker.trackerdata.SOURCE_WATCH
import com.foxlab.procrastinationtracker.trackerdata.LiveSessionState
import com.foxlab.procrastinationtracker.trackerdata.LiveSessionSync
import com.foxlab.procrastinationtracker.trackerdata.ProfileWeeklyBreakdown
import com.foxlab.procrastinationtracker.trackerdata.RemoteLiveSession
import com.foxlab.procrastinationtracker.trackerdata.SliceDraft
import com.foxlab.procrastinationtracker.trackerdata.WeekSummary
import com.foxlab.procrastinationtracker.trackerdata.dao.SliceTotal
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.LayoutProfileEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackerViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as ProcrastinationTrackerApp).trackerRepository

    data class UiState(
        val profiles: List<LayoutProfileEntity> = emptyList(),
        val activeProfile: LayoutProfileEntity? = null,
        val slices: List<ActivitySliceEntity> = emptyList(),
        val todayTotals: Map<String, Long> = emptyMap(),
        /** Everything tracked today across every profile -- what the board headline shows. */
        val todayAllProfilesMillis: Long = 0L,
        val weekTotals: Map<String, Long> = emptyMap(),
        val totalTrackedMillis: Long = 0L,
        val currentWeekSummary: WeekSummary? = null,
        val pastWeeksSummaries: List<WeekSummary> = emptyList(),
        val profileBreakdowns: List<ProfileWeeklyBreakdown> = emptyList(),
        /** Time recorded on the watch, shown as its own report tab when there is any. */
        val watchBreakdown: DeviceBreakdown? = null,
        val service: TrackerForegroundService.UiState = TrackerForegroundService.UiState()
    ) {
        /** Today's total for a slice, including the live elapsed time if it's the active one. */
        fun liveTodayTotal(sliceId: String): Long {
            val base = todayTotals[sliceId] ?: 0L
            return if (service.activeSliceId == sliceId) base + service.elapsedMillis else base
        }

        /**
         * The report only earns its place once there is at least a minute recorded -- below that
         * every card reads "0m" and the screen is pure noise (spec 002 has no report for an empty
         * database, and the reference app doesn't show one either).
         */
        val hasReportableHistory: Boolean get() = totalTrackedMillis >= ActivityRules.MIN_REPORTABLE_MILLIS

        /** Total milliseconds tracked today across all slices. */
        val todayTotalMillis: Long get() {
            var sum = todayTotals.values.sum()
            if (service.isTracking) {
                sum += service.elapsedMillis
            }
            return sum
        }
    }

    private val activeProfileFlow = repository.observeActiveProfile()

    private data class SlicesAndTotals(
        val slices: List<ActivitySliceEntity>,
        val today: List<SliceTotal>,
        val week: List<SliceTotal>
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val slicesAndTotalsFlow = activeProfileFlow.flatMapLatest { profile ->
        if (profile == null) {
            flowOf(SlicesAndTotals(emptyList(), emptyList(), emptyList()))
        } else {
            combine(
                repository.observeSlices(profile.id),
                repository.observeTodayTotals(profile.id),
                repository.observeSundayWeekTotals(profile.id)
            ) { slices, today, week -> SlicesAndTotals(slices, today, week) }
        }
    }

    private data class CoreState(
        val profiles: List<LayoutProfileEntity>,
        val activeProfile: LayoutProfileEntity?,
        val slicesAndTotals: SlicesAndTotals,
        val service: TrackerForegroundService.UiState,
        val todayAllProfilesMillis: Long
    )

    private val coreStateFlow = combine(
        repository.observeProfiles(),
        activeProfileFlow,
        slicesAndTotalsFlow,
        TrackerForegroundService.uiState,
        repository.observeTodayTotalAllProfiles()
    ) { profiles, active, slicesAndTotals, service, todayAll ->
        CoreState(profiles, active, slicesAndTotals, service, todayAll)
    }

    private val watchBreakdownFlow = repository.observeDeviceBreakdown(SOURCE_WATCH)

    val uiState = combine(
        coreStateFlow,
        repository.observeWeeklySummaries(),
        repository.observeAllProfilesWeeklyBreakdown(),
        repository.observeTotalTrackedMillis(),
        watchBreakdownFlow
    ) { core, weeklySummaries, breakdowns, totalTracked, watch ->
        val currentWeek = weeklySummaries.firstOrNull { it.isCurrentWeek }
        val pastWeeks = weeklySummaries.filter { !it.isCurrentWeek }

        UiState(
            profiles = core.profiles,
            activeProfile = core.activeProfile,
            slices = core.slicesAndTotals.slices,
            todayTotals = core.slicesAndTotals.today.associate { it.sliceId to it.totalMillis },
            todayAllProfilesMillis = core.todayAllProfilesMillis,
            weekTotals = core.slicesAndTotals.week.associate { it.sliceId to it.totalMillis },
            totalTrackedMillis = totalTracked,
            currentWeekSummary = currentWeek,
            pastWeeksSummaries = pastWeeks,
            profileBreakdowns = breakdowns,
            watchBreakdown = watch.takeIf { it.hasAnything },
            service = core.service
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun activateSlice(slice: ActivitySliceEntity) {
        val intent = Intent(getApplication(), TrackerForegroundService::class.java).apply {
            action = TrackerForegroundService.ACTION_ACTIVATE_SLICE
            putExtra(TrackerForegroundService.EXTRA_SLICE_ID, slice.id)
            putExtra(TrackerForegroundService.EXTRA_SLICE_TITLE, slice.title)
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    /** Tapping the slice that's already active. */
    fun pauseActive() = sendAction(TrackerForegroundService.ACTION_PAUSE)

    fun stopActive() = sendAction(TrackerForegroundService.ACTION_STOP)

    fun discardActive() = sendAction(TrackerForegroundService.ACTION_DISCARD)

    fun switchProfile(profileId: String) {
        val intent = Intent(getApplication(), TrackerForegroundService::class.java).apply {
            action = TrackerForegroundService.ACTION_SWITCH_PROFILE
            putExtra(TrackerForegroundService.EXTRA_PROFILE_ID, profileId)
        }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    private fun sendAction(action: String) {
        val intent = Intent(getApplication(), TrackerForegroundService::class.java).apply { this.action = action }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    // --- Profile / slice management (no service involvement, straight to the repository) ---

    fun createCustomFromScratch(title: String, slices: List<SliceDraft>, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { repository.createCustomFromScratchWithDrafts(title, slices) })
        }
    }

    fun forkToCustom(sourceProfileId: String, editedSlices: List<SliceDraft>, desiredTitle: String?, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { repository.forkToCustomWithDrafts(sourceProfileId, editedSlices, desiredTitle) })
        }
    }

    fun renameCustomProfile(profileId: String, newTitle: String) {
        viewModelScope.launch { runCatching { repository.renameCustomProfile(profileId, newTitle) } }
    }

    fun deleteCustomProfile(profileId: String) {
        viewModelScope.launch { runCatching { repository.deleteCustomProfile(profileId) } }
    }

    fun updateSlice(sliceId: String, newTitle: String, newIconKey: String?) {
        viewModelScope.launch { runCatching { repository.updateSlice(sliceId, newTitle, newIconKey) } }
    }

    /** "Reiniciar contagem": clears today for this profile only. */
    fun resetTodayForProfile(profileId: String) {
        viewModelScope.launch {
            runCatching { repository.resetTodayForProfile(profileId) }
            pushToWatch()
        }
    }

    /** "Corrigir tempo": rewrites today's total of one activity. */
    fun setTodayTotal(sliceId: String, totalMillis: Long) {
        viewModelScope.launch {
            runCatching { repository.setTodayTotalForSlice(sliceId, totalMillis) }
            pushToWatch()
        }
    }

    /**
     * Corrections and resets push right away instead of waiting for the two-minute safety net.
     * Measured in the real round trip: fixing the day on the phone and watching the wrist keep
     * the old number for minutes reads as the sync being broken, even though it would catch up.
     */
    private suspend fun pushToWatch() {
        runCatching { ActivitySyncSender.push(getApplication(), repository) }
    }

    /**
     * The other device's running block, if it is still fresh and this device isn't tracking.
     * Checked when the screen opens, which is exactly when the user is deciding where to count.
     */
    suspend fun remoteRunningSession(): LiveSessionState? {
        if (uiState.value.service.isTracking) return null
        // Never heard a "hi" from a companion: nothing to ask, and no reason to pay for a Data
        // Layer lookup that has nobody to answer it.
        if (!CompanionPresence.hasCompanion(getApplication())) return null
        val myId = LiveSessionSync.deviceId(getApplication())
        RemoteLiveSession.runningOnOtherDevice(myId)?.let { return it }
        // Nothing cached (fresh process): ask the Data Layer for the current broadcast.
        val fetched = withContext(Dispatchers.IO) { LiveSessionSync.readRemote(getApplication()) }
        if (fetched != null) RemoteLiveSession.update(fetched)
        return fetched?.takeIf { it.isFresh() && it.deviceId != myId }
    }

    /** "Continuar aqui": same block, same start time, new owner. */
    fun takeOverRemoteSession(remote: LiveSessionState) {
        val intent = Intent(getApplication(), TrackerForegroundService::class.java).apply {
            action = TrackerForegroundService.ACTION_TAKE_OVER
            putExtra(TrackerForegroundService.EXTRA_TAKEOVER_SESSION_ID, remote.sessionId)
            putExtra(TrackerForegroundService.EXTRA_SLICE_ID, remote.sliceId)
            putExtra(TrackerForegroundService.EXTRA_SLICE_TITLE, remote.sliceTitle)
            putExtra(TrackerForegroundService.EXTRA_TAKEOVER_STARTED_AT, remote.startedAtMillis)
        }
        ContextCompat.startForegroundService(getApplication(), intent)
        RemoteLiveSession.clear()
    }

    /**
     * "Começar um novo": the other device's block is closed *and saved* here (the id is shared, so
     * writing the closed row locally is enough -- it syncs back and the other side stops counting
     * when it sees this device took the session over).
     */
    fun closeRemoteAndStartNew(remote: LiveSessionState, slice: ActivitySliceEntity) {
        viewModelScope.launch {
            // "Começar um novo": the other device closes and saves its own block when it sees
            // this device take over, so nothing is written on its behalf here.
            activateSlice(slice)
            pushToWatch()
        }
        RemoteLiveSession.clear()
    }

    fun renameSlice(sliceId: String, newTitle: String) {
        viewModelScope.launch { runCatching { repository.renameSlice(sliceId, newTitle) } }
    }

    fun addSliceToCustomProfile(profileId: String, title: String, iconKey: String? = null, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { repository.addSliceToCustomProfile(profileId, title, iconKey) })
        }
    }

    fun removeSliceFromCustomProfile(sliceId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { repository.removeSliceFromCustomProfile(sliceId) })
        }
    }
}
