package com.foxlab.procrastinationtracker.trackerdata

import com.foxlab.procrastinationtracker.core.ActivityRules
import com.foxlab.procrastinationtracker.trackerdata.dao.SessionWithSlice
import com.foxlab.procrastinationtracker.trackerdata.dao.SliceTotal
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySessionEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.DeletedSessionEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.LayoutProfileEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.ProfileType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.text.Normalizer
import java.util.Calendar
import java.util.UUID

/** Max simultaneous Custom profiles, per spec §2. */
const val MAX_CUSTOM_PROFILES = 10

/** Min/max slices per profile. Duo/Tri are additionally locked at exactly 2/3. */
const val MIN_SLICES = 2
const val MAX_SLICES = 10

private const val DUO_ID = "duo"
private const val TRI_ID = "tri"

/**
 * Seeded activities use a deterministic id instead of a random UUID. Each device seeds Duo/Tri
 * on its own first run, so with random ids the phone's "Foco" and the watch's "Foco" were two
 * different rows -- and the first sync would have inserted both, leaving the profile with two
 * of everything and the time split between them. Same id on both sides makes that merge a no-op.
 *
 * The id is tied to the position, not the title: renaming "Foco" to "Estudo" must not change
 * which row it is.
 */
fun seededSliceId(profileId: String, position: Int): String = "$profileId-slice-$position"
private val DUO_DEFAULT_SLICES = listOf("Foco" to "focus", "Procrastinando" to "coffee")
private val TRI_DEFAULT_SLICES = listOf("Trabalho" to "work", "Estudo" to "school", "Procrastinando" to "coffee")

/** How long a running session can go without a heartbeat before we assume the process died. */
private const val STALE_SESSION_THRESHOLD_MILLIS = 2 * 60 * 1000L

/** How often the "today"/"this week" window start is re-checked while a screen is observing. */
private const val BOUNDARY_POLL_MILLIS = 60 * 1000L

data class SliceDraft(val title: String, val iconKey: String? = null)

/** Devices that can record a session. Kept as strings in the entity for forward compatibility. */
const val SOURCE_PHONE = "phone"
const val SOURCE_WATCH = "watch"

data class WeekSummary(
    val startSundayMillis: Long,
    val endSaturdayMillis: Long,
    val isCurrentWeek: Boolean,
    val activeMillis: Long,
    val procrastinatingMillis: Long,
    val totalMillis: Long,
    val procrastinationPercent: Int,
    /** Where the week's time was recorded -- the watch keeps counting when the phone is charging. */
    val phoneMillis: Long = 0L,
    val watchMillis: Long = 0L
)

data class ProfileWeeklyBreakdown(
    val profile: LayoutProfileEntity,
    val slicesWithTotals: List<SliceWithTotal>,
    val totalMillis: Long,
    /** Consolidated weeks before this one, for this profile only. */
    val pastWeeks: List<WeekSummary> = emptyList()
)

/**
 * Everything recorded by one device (the watch, typically), as its own report tab. Kept separate
 * from the per-profile breakdown because "where did this time come from" is a different question
 * from "which profile was it": a run tracked on the wrist and the study block tracked on the
 * phone can belong to the same profile.
 */
data class DeviceBreakdown(
    val deviceKind: String,
    val slicesWithTotals: List<SliceWithTotal>,
    val totalMillis: Long,
    val pastWeeks: List<WeekSummary> = emptyList()
) {
    val hasAnything: Boolean get() = totalMillis > 0 || pastWeeks.any { it.totalMillis > 0 }
}

/** A payload plus the rows it covered, so the caller can mark them as sent once it lands. */
data class SyncSnapshot(
    val payload: TrackerSyncPayload,
    val sessionIds: List<String>,
    val wasFull: Boolean
)

data class SliceWithTotal(
    val slice: ActivitySliceEntity,
    val totalMillis: Long
)

class TrackerRepository(private val db: TrackerDatabase) {

    private val profileDao = db.layoutProfileDao()
    private val sliceDao = db.activitySliceDao()
    private val sessionDao = db.activitySessionDao()
    private val deletedSessionDao = db.deletedSessionDao()

    // ---------------------------------------------------------------------
    // Seeding
    // ---------------------------------------------------------------------

    /** Creates Duo and Tri (with their default slices) the first time the app runs. Idempotent. */
    suspend fun ensureSeeded() {
        if (profileDao.getById(DUO_ID) != null) return
        val now = System.currentTimeMillis()

        profileDao.insert(
            LayoutProfileEntity(DUO_ID, ProfileType.DUO, "Duo", isActive = true, forkedFromProfileId = null, createdAt = now, updatedAt = now)
        )
        sliceDao.insertAll(defaultSlicesWithIcons(DUO_ID, DUO_DEFAULT_SLICES, now))

        profileDao.insert(
            LayoutProfileEntity(TRI_ID, ProfileType.TRI, "Tri", isActive = false, forkedFromProfileId = null, createdAt = now, updatedAt = now)
        )
        sliceDao.insertAll(defaultSlicesWithIcons(TRI_ID, TRI_DEFAULT_SLICES, now))
    }

    private fun defaultSlicesWithIcons(profileId: String, items: List<Pair<String, String?>>, now: Long): List<ActivitySliceEntity> =
        items.mapIndexed { index, (title, iconKey) ->
            ActivitySliceEntity(
                id = seededSliceId(profileId, index),
                profileId = profileId,
                title = title,
                color = null,
                position = index,
                timerModeId = null,
                iconKey = iconKey,
                createdAt = now,
                updatedAt = now
            )
        }

    // ---------------------------------------------------------------------
    // Profiles
    // ---------------------------------------------------------------------

    fun observeProfiles(): Flow<List<LayoutProfileEntity>> = profileDao.observeAll()

    fun observeActiveProfile(): Flow<LayoutProfileEntity?> = profileDao.observeActive()

    fun observeSlices(profileId: String): Flow<List<ActivitySliceEntity>> = sliceDao.observeByProfile(profileId)

    /** Every profile's activities at once -- used to know which profiles fit on the watch. */
    fun observeAllActiveSlices(): Flow<List<ActivitySliceEntity>> = sliceDao.observeAllActiveSlices()

    /**
     * One-shot reads for callers that aren't a UI -- the foreground service needs to know what
     * "the next activity" is when the user cycles from the notification, without collecting a flow.
     */
    suspend fun getActiveProfileOnce(): LayoutProfileEntity? = profileDao.getAllOnce().firstOrNull { it.isActive }

    suspend fun getSlicesOnce(profileId: String): List<ActivitySliceEntity> = sliceDao.getByProfile(profileId)

    suspend fun getSliceById(sliceId: String): ActivitySliceEntity? = sliceDao.getById(sliceId)

    /** The activity after [currentSliceId] in the active profile, wrapping around. */
    suspend fun getNextSlice(currentSliceId: String?): ActivitySliceEntity? {
        val profile = getActiveProfileOnce() ?: return null
        val slices = getSlicesOnce(profile.id)
        if (slices.isEmpty()) return null
        val index = slices.indexOfFirst { it.id == currentSliceId }
        return if (index < 0) slices.first() else slices[(index + 1) % slices.size]
    }

    /** Switching profiles always closes whatever session is running first (spec §4.1). */
    suspend fun switchActiveProfile(profileId: String) {
        pauseActive()
        profileDao.setActive(profileId)
    }

    /**
     * A user edited a Duo/Tri (or another Custom) slice and hit save: Duo/Tri themselves are
     * never mutated, this always creates a brand new Custom profile instead.
     */
    suspend fun forkToCustom(sourceProfileId: String, editedTitles: List<String>, desiredTitle: String? = null): String {
        return forkToCustomWithDrafts(sourceProfileId, editedTitles.map { SliceDraft(it, null) }, desiredTitle)
    }

    suspend fun forkToCustomWithDrafts(sourceProfileId: String, editedSlices: List<SliceDraft>, desiredTitle: String? = null): String {
        check(profileDao.countCustom() < MAX_CUSTOM_PROFILES) { "Limite de $MAX_CUSTOM_PROFILES perfis Custom atingido" }
        val source = requireNotNull(profileDao.getById(sourceProfileId)) { "Perfil de origem não encontrado" }
        val sourceSlices = sliceDao.getByProfile(sourceProfileId)
        require(editedSlices.size == sourceSlices.size) { "Quantidade de fatias não pode mudar ao copiar de ${source.title}" }

        val title = uniqueTitle(desiredTitle?.takeIf { it.isNotBlank() } ?: suggestCopyName(source.title))
        return createProfileWithDrafts(ProfileType.CUSTOM, title, editedSlices, forkedFrom = sourceProfileId)
    }

    /** Creates a Custom profile from scratch, 2 to 10 slices, no relation to Duo/Tri. */
    suspend fun createCustomFromScratch(title: String, sliceTitles: List<String>): String {
        return createCustomFromScratchWithDrafts(title, sliceTitles.map { SliceDraft(it, null) })
    }

    suspend fun createCustomFromScratchWithDrafts(title: String, slices: List<SliceDraft>): String {
        check(profileDao.countCustom() < MAX_CUSTOM_PROFILES) { "Limite de $MAX_CUSTOM_PROFILES perfis Custom atingido" }
        require(slices.size in MIN_SLICES..MAX_SLICES) { "Um perfil precisa ter entre $MIN_SLICES e $MAX_SLICES fatias" }
        return createProfileWithDrafts(ProfileType.CUSTOM, uniqueTitle(title), slices, forkedFrom = null)
    }

    private suspend fun createProfileWithDrafts(type: ProfileType, title: String, slices: List<SliceDraft>, forkedFrom: String?): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        profileDao.insert(LayoutProfileEntity(id, type, title, isActive = false, forkedFromProfileId = forkedFrom, createdAt = now, updatedAt = now))
        sliceDao.insertAll(slices.mapIndexed { index, draft ->
            ActivitySliceEntity(
                id = UUID.randomUUID().toString(),
                profileId = id,
                title = draft.title,
                color = null,
                position = index,
                timerModeId = null,
                iconKey = draft.iconKey,
                createdAt = now,
                updatedAt = now
            )
        })
        return id
    }

    /** "Tri" -> "Tri (cópia)", then "Tri (cópia_01)", "Tri (cópia_02)"... to avoid collisions. */
    private suspend fun suggestCopyName(baseTitle: String): String {
        val existing = profileDao.allTitles().toSet()
        val plain = "$baseTitle (cópia)"
        if (plain !in existing) return plain
        var n = 1
        while (true) {
            val candidate = "$baseTitle (cópia_%02d)".format(n)
            if (candidate !in existing) return candidate
            n++
        }
    }

    private suspend fun uniqueTitle(desired: String): String {
        val existing = profileDao.allTitles().toSet()
        if (desired !in existing) return desired
        var n = 1
        while (true) {
            val candidate = "$desired ($n)"
            if (candidate !in existing) return candidate
            n++
        }
    }

    suspend fun renameCustomProfile(profileId: String, newTitle: String) {
        val profile = requireNotNull(profileDao.getById(profileId)) { "Perfil não encontrado" }
        require(profile.type == ProfileType.CUSTOM) { "Só é possível renomear perfis Custom" }
        profileDao.update(profile.copy(title = uniqueTitle(newTitle), updatedAt = System.currentTimeMillis()))
    }

    /** Deletes a Custom profile and all its slices/history. Duo/Tri can never be deleted. */
    suspend fun deleteCustomProfile(profileId: String) {
        val profile = requireNotNull(profileDao.getById(profileId)) { "Perfil não encontrado" }
        require(profile.type == ProfileType.CUSTOM) { "Duo e Tri não podem ser excluídos" }
        if (profile.isActive) {
            pauseActive()
            profileDao.setActive(DUO_ID)
        }
        sessionDao.deleteByProfile(profileId)
        sliceDao.deleteByProfile(profileId)
        profileDao.deleteCustom(profileId)
    }

    // ---------------------------------------------------------------------
    // Slices within a Custom profile
    // ---------------------------------------------------------------------

    suspend fun renameSlice(sliceId: String, newTitle: String) {
        val slice = requireNotNull(sliceDao.getById(sliceId)) { "Fatia não encontrada" }
        sliceDao.update(slice.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateSlice(sliceId: String, newTitle: String, newIconKey: String?) {
        val slice = requireNotNull(sliceDao.getById(sliceId)) { "Fatia não encontrada" }
        sliceDao.update(slice.copy(title = newTitle, iconKey = newIconKey, updatedAt = System.currentTimeMillis()))
    }

    suspend fun addSliceToCustomProfile(profileId: String, title: String, iconKey: String? = null) {
        val profile = requireNotNull(profileDao.getById(profileId)) { "Perfil não encontrado" }
        require(profile.type == ProfileType.CUSTOM) { "Só dá para adicionar fatias em perfis Custom" }
        val current = sliceDao.getByProfile(profileId)
        require(current.size < MAX_SLICES) { "Máximo de $MAX_SLICES fatias por perfil" }
        val now = System.currentTimeMillis()
        sliceDao.insert(
            ActivitySliceEntity(UUID.randomUUID().toString(), profileId, title, null, current.size, null, iconKey = iconKey, createdAt = now, updatedAt = now)
        )
    }

    suspend fun removeSliceFromCustomProfile(sliceId: String) {
        val slice = requireNotNull(sliceDao.getById(sliceId)) { "Fatia não encontrada" }
        val profile = requireNotNull(profileDao.getById(slice.profileId)) { "Perfil não encontrado" }
        require(profile.type == ProfileType.CUSTOM) { "Só dá para remover fatias em perfis Custom" }
        val current = sliceDao.getByProfile(slice.profileId)
        require(current.size > MIN_SLICES) { "Um perfil precisa ter pelo menos $MIN_SLICES fatias" }
        sliceDao.archive(sliceId, System.currentTimeMillis())
    }

    // ---------------------------------------------------------------------
    // Active session lifecycle (see spec §4.1 and §6)
    // ---------------------------------------------------------------------

    /** Tapping an inactive slice: closes whatever was running, starts tracking this one. */
    suspend fun activateSlice(sliceId: String, sourceDevice: String): String {
        val now = System.currentTimeMillis()
        closeRunningSession(now)
        val id = UUID.randomUUID().toString()
        sessionDao.insert(
            ActivitySessionEntity(id, sliceId, subtaskId = null, startTime = now, endTime = null, lastHeartbeatAt = now, sourceDevice = sourceDevice, createdAt = now)
        )
        return id
    }

    /**
     * Continues, on this device, a block that was running on the other one.
     *
     * Deliberately *not* the same row changing hands: the other device closes and saves the time
     * it actually measured, and this device opens a new block that remembers where it came from
     * ([ActivitySessionEntity.continuedFromSessionId]) plus how much was already banked
     * ([ActivitySessionEntity.carriedMillis]). Two rows, two true per-device measurements, one
     * continuous number for the user -- and if this device dies mid-block, the time already
     * counted elsewhere is safe on disk instead of disappearing with the open row.
     */
    suspend fun startContinuationSession(
        sliceId: String,
        sourceDevice: String,
        carriedMillis: Long,
        continuedFromSessionId: String?
    ): String {
        val now = System.currentTimeMillis()
        closeRunningSession(now)
        val id = UUID.randomUUID().toString()
        sessionDao.insert(
            ActivitySessionEntity(
                id = id,
                sliceId = sliceId,
                subtaskId = null,
                startTime = now,
                endTime = null,
                lastHeartbeatAt = now,
                sourceDevice = sourceDevice,
                continuedFromSessionId = continuedFromSessionId,
                carriedMillis = carriedMillis.coerceAtLeast(0L),
                createdAt = now
            )
        )
        return id
    }

    /**
     * The other device is taking this block over: close and *save* what was measured here. The
     * user sees one continuous clock, but this row is the honest record of the time this device
     * counted -- which is what lets the reports separate watch time from phone time.
     */
    suspend fun handOverRunningSession(): Long {
        val running = sessionDao.getRunning() ?: return 0L
        val now = System.currentTimeMillis()
        sessionDao.close(running.id, now)
        return (now - running.startTime).coerceAtLeast(0L)
    }

    /** Tapping the already-active slice, the notification's "Pausar", or the back-menu "Pausar". */
    suspend fun pauseActive() {
        closeRunningSession(System.currentTimeMillis())
    }

    /** Same data effect as pause; kept distinct so callers (the service) can also tear down the notification. */
    suspend fun stopActive() {
        closeRunningSession(System.currentTimeMillis())
    }

    /** Back-menu "Reiniciar": discards the in-progress session, nothing is saved. */
    suspend fun discardActiveSession() {
        sessionDao.getRunning()?.let { sessionDao.discard(it.id) }
    }

    suspend fun getRunningSession(): ActivitySessionEntity? = sessionDao.getRunning()

    suspend fun heartbeatActive() {
        sessionDao.getRunning()?.let { sessionDao.heartbeat(it.id, System.currentTimeMillis()) }
    }

    private suspend fun closeRunningSession(now: Long) {
        sessionDao.getRunning()?.let { sessionDao.close(it.id, now) }
    }

    /**
     * Call once on app/service startup. Closes any session abandoned by a crash: if its last
     * heartbeat (or start time, if it never got one) is older than the stale threshold, we
     * assume the process died and close it using that last known timestamp so at most ~30s of
     * tracked time is lost, never a whole day (spec §6).
     */
    suspend fun recoverStaleSessions() {
        val now = System.currentTimeMillis()
        sessionDao.getAllUnclosed().forEach { session ->
            val lastKnown = session.lastHeartbeatAt ?: session.startTime
            if (now - lastKnown > STALE_SESSION_THRESHOLD_MILLIS) {
                sessionDao.close(session.id, lastKnown)
            }
        }
    }

    // ---------------------------------------------------------------------
    // Correcting the day (forgot to switch activities, wrong reading, fresh start)
    // ---------------------------------------------------------------------

    /**
     * Wipes today's tracking for one profile only. Other profiles keep their sessions, so the
     * day headline simply recomputes without this profile's time instead of going to zero.
     */
    suspend fun resetTodayForProfile(profileId: String) {
        pauseActive()
        val dayStart = startOfTodayMillis()
        rememberDeleted(sessionDao.getIdsForProfileSince(profileId, dayStart))
        sessionDao.deleteForProfileSince(profileId, dayStart)
    }

    /**
     * Rewrites today's total for one activity to [totalMillis] -- "I actually studied 2h, not 4h".
     * Today's rows for that activity are replaced by a single block ending now, clamped so it can
     * never claim more time than the day has had so far.
     */
    suspend fun setTodayTotalForSlice(sliceId: String, totalMillis: Long, sourceDevice: String = "phone") {
        pauseActive()
        val now = System.currentTimeMillis()
        val dayStart = startOfTodayMillis(now)
        val clamped = totalMillis.coerceIn(0L, now - dayStart)
        rememberDeleted(sessionDao.getIdsForSliceSince(sliceId, dayStart))
        sessionDao.deleteForSliceSince(sliceId, dayStart)
        if (clamped <= 0L) return
        sessionDao.insert(
            ActivitySessionEntity(
                id = UUID.randomUUID().toString(),
                sliceId = sliceId,
                subtaskId = null,
                startTime = now - clamped,
                endTime = now,
                lastHeartbeatAt = now,
                sourceDevice = sourceDevice,
                createdAt = now
            )
        )
    }

    /**
     * Records that these sessions were removed on purpose, so the other device deletes them too
     * instead of pushing them back on the next merge.
     */
    private suspend fun rememberDeleted(sessionIds: List<String>) {
        if (sessionIds.isEmpty()) return
        val now = System.currentTimeMillis()
        deletedSessionDao.insertAll(sessionIds.map { DeletedSessionEntity(sessionId = it, deletedAt = now) })
    }

    // ---------------------------------------------------------------------
    // Reports & Weekly Consolidation
    // ---------------------------------------------------------------------

    /**
     * "Today" and "this week" are moving targets: with the app left open across midnight the old
     * code kept summing yesterday's window until the process restarted. These flows re-emit the
     * boundary as it is crossed, and everything downstream recomputes.
     */
    private fun dayStartFlow(): Flow<Long> = boundaryFlow { startOfTodayMillis(it) }

    private fun weekStartFlow(): Flow<Long> = boundaryFlow { getStartOfWeekSunday(it) }

    /**
     * Recomputes the window start once a minute and only emits when it actually moves. Polling
     * (instead of sleeping until the exact boundary) keeps this correct across DST shifts and
     * manual clock changes, and it only runs while something is collecting.
     */
    private fun boundaryFlow(windowStart: (Long) -> Long): Flow<Long> = flow {
        while (true) {
            emit(windowStart(System.currentTimeMillis()))
            delay(BOUNDARY_POLL_MILLIS)
        }
    }.distinctUntilChanged()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeTodayTotals(profileId: String): Flow<List<SliceTotal>> =
        dayStartFlow().flatMapLatest { start -> sliceDao.observeTotalsSince(profileId, start) }

    /** Week totals starting from Sunday 00:00 of the current week. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeSundayWeekTotals(profileId: String): Flow<List<SliceTotal>> =
        weekStartFlow().flatMapLatest { start -> sliceDao.observeTotalsSince(profileId, start) }

    /**
     * Housekeeping for the watch: it is a companion screen, not an archive. Anything older than
     * [keepMillis] already lives on the phone, and trimming it keeps the sync payload inside the
     * Data Layer's size budget, which a forever-growing snapshot would eventually blow past.
     */
    suspend fun pruneSessionsOlderThan(keepMillis: Long) {
        sessionDao.deleteClosedBefore(System.currentTimeMillis() - keepMillis)
    }

    /**
     * Everything tracked today, across every profile. Switching from Duo to Tri at lunch doesn't
     * mean the morning didn't happen, so the "today" headline is profile-agnostic.
     */
    fun observeTodayTotalAllProfiles(): Flow<Long> =
        combine(dayStartFlow(), sessionDao.observeAllClosedSessionsWithSlice()) { dayStart, sessions ->
            sessions.filter { it.startTime >= dayStart }
                .sumOf { (it.endTime - it.startTime).coerceAtLeast(0L) }
        }

    fun observeWeeklySummaries(): Flow<List<WeekSummary>> =
        combine(weekStartFlow(), sessionDao.observeAllClosedSessionsWithSlice()) { _, sessions -> sessions }.map { sessions ->
            summarizeWeeks(sessions, getStartOfWeekSunday(), includeEmptyCurrentWeek = true)
        }

    /**
     * Groups closed sessions into Sunday-to-Saturday weeks. Shared by the overall report and by
     * each profile's tab, so "productive vs. procrastinating" is computed one way only.
     */
    private fun summarizeWeeks(
        sessions: List<SessionWithSlice>,
        currentWeekSunday: Long,
        includeEmptyCurrentWeek: Boolean
    ): List<WeekSummary> {
        val sessionsByWeek = sessions.groupBy { getStartOfWeekSunday(it.startTime) }
        val sundays = if (includeEmptyCurrentWeek) {
            (sessionsByWeek.keys + currentWeekSunday).distinct()
        } else {
            sessionsByWeek.keys.toList()
        }.sortedDescending()

        return sundays.map { weekSunday ->
            var activeMillis = 0L
            var procrastinatingMillis = 0L
            var phoneMillis = 0L
            var watchMillis = 0L
            for (s in sessionsByWeek[weekSunday].orEmpty()) {
                val duration = (s.endTime - s.startTime).coerceAtLeast(0L)
                if (isProcrastinationTitle(s.sliceTitle)) procrastinatingMillis += duration
                else activeMillis += duration
                if (s.sourceDevice == SOURCE_WATCH) watchMillis += duration else phoneMillis += duration
            }
            val totalMillis = activeMillis + procrastinatingMillis
            WeekSummary(
                startSundayMillis = weekSunday,
                endSaturdayMillis = weekSunday + 7L * 24 * 60 * 60 * 1000 - 1L,
                isCurrentWeek = (weekSunday == currentWeekSunday),
                activeMillis = activeMillis,
                procrastinatingMillis = procrastinatingMillis,
                totalMillis = totalMillis,
                procrastinationPercent = if (totalMillis > 0) ((procrastinatingMillis * 100) / totalMillis).toInt() else 0,
                phoneMillis = phoneMillis,
                watchMillis = watchMillis
            )
        }
    }

    /**
     * This week per activity, plus the closed weeks, for a single recording device. Only emitted
     * as a tab when it actually has time -- a user who never wore the watch shouldn't see an
     * empty "Relógio" tab.
     */
    /**
     * Today, split by which device recorded it: `device -> (sliceId -> millis)`. The watch uses
     * this to answer "and what did I do on the phone today?" without shipping the whole report
     * to a 1.4" screen.
     */
    fun observeTodayTotalsByDevice(): Flow<Map<String, Map<String, Long>>> =
        combine(dayStartFlow(), sessionDao.observeAllClosedSessionsWithSlice()) { dayStart, sessions ->
            sessions
                .filter { it.startTime >= dayStart }
                .groupBy { it.sourceDevice }
                .mapValues { (_, list) ->
                    list.groupBy { it.sliceId }
                        .mapValues { (_, rows) -> rows.sumOf { (it.endTime - it.startTime).coerceAtLeast(0L) } }
                }
        }

    fun observeDeviceBreakdown(deviceKind: String): Flow<DeviceBreakdown> {
        return combine(
            sliceDao.observeAllActiveSlices(),
            sessionDao.observeAllClosedSessionsWithSlice(),
            weekStartFlow()
        ) { slices, sessions, weekStart ->
            val deviceSessions = sessions.filter { it.sourceDevice == deviceKind }
            val weekSessions = deviceSessions.filter { it.startTime >= weekStart }
            val totalsBySliceId = weekSessions.groupBy { it.sliceId }
                .mapValues { (_, list) -> list.sumOf { (it.endTime - it.startTime).coerceAtLeast(0L) } }

            val slicesWithTotals = slices
                .filter { totalsBySliceId.containsKey(it.id) }
                .sortedBy { it.position }
                .map { SliceWithTotal(slice = it, totalMillis = totalsBySliceId[it.id] ?: 0L) }

            DeviceBreakdown(
                deviceKind = deviceKind,
                slicesWithTotals = slicesWithTotals,
                totalMillis = slicesWithTotals.sumOf { it.totalMillis },
                pastWeeks = summarizeWeeks(deviceSessions, weekStart, includeEmptyCurrentWeek = false)
                    .filter { !it.isCurrentWeek }
            )
        }
    }

    fun observeAllProfilesWeeklyBreakdown(): Flow<List<ProfileWeeklyBreakdown>> {
        return combine(
            profileDao.observeAll(),
            sliceDao.observeAllActiveSlices(),
            sessionDao.observeAllClosedSessionsWithSlice(),
            weekStartFlow()
        ) { profiles, slices, sessions, weekStart ->
            val weekSessions = sessions.filter { it.startTime >= weekStart }
            val totalsBySliceId = weekSessions.groupBy { it.sliceId }
                .mapValues { (_, sList) -> sList.sumOf { (it.endTime - it.startTime).coerceAtLeast(0L) } }
            val slicesByProfile = slices.groupBy { it.profileId }
            val sessionsByProfile = sessions.groupBy { it.profileId }

            profiles.map { profile ->
                val profileSlices = slicesByProfile[profile.id].orEmpty().sortedBy { it.position }
                val slicesWithTotals = profileSlices.map { slice ->
                    SliceWithTotal(
                        slice = slice,
                        totalMillis = totalsBySliceId[slice.id] ?: 0L
                    )
                }
                ProfileWeeklyBreakdown(
                    profile = profile,
                    slicesWithTotals = slicesWithTotals,
                    totalMillis = slicesWithTotals.sumOf { it.totalMillis },
                    pastWeeks = summarizeWeeks(
                        sessions = sessionsByProfile[profile.id].orEmpty(),
                        currentWeekSunday = weekStart,
                        includeEmptyCurrentWeek = false
                    ).filter { !it.isCurrentWeek }
                )
            }
        }
    }

    /**
     * Everything ever tracked, summed. The UI uses this to decide whether a report is worth
     * showing at all: a brand new install (or a couple of accidental taps) has nothing to plot,
     * and a screen full of "0m" is worse than no screen.
     */
    fun observeTotalTrackedMillis(): Flow<Long> =
        sessionDao.observeAllClosedSessionsWithSlice().map { sessions ->
            sessions.sumOf { (it.endTime - it.startTime).coerceAtLeast(0L) }
        }

    companion object {
        fun getStartOfWeekSunday(nowMillis: Long = System.currentTimeMillis()): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = nowMillis
            cal.firstDayOfWeek = Calendar.SUNDAY
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val daysFromSunday = dayOfWeek - Calendar.SUNDAY
            cal.add(Calendar.DAY_OF_MONTH, -daysFromSunday)
            return cal.timeInMillis
        }

        fun startOfTodayMillis(nowMillis: Long = System.currentTimeMillis()): Long = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        /** Delegates to the shared rule in `:core` so phone, watch and reports never disagree. */
        fun isProcrastinationTitle(title: String): Boolean = ActivityRules.isProcrastination(title)
    }

    // ---------------------------------------------------------------------
    // Sync (spec §7) — full-snapshot push/merge over the Wearable Data Layer.
    // Callers on each platform own the actual DataClient calls; this repository
    // only knows how to build a payload and how to merge one it received.
    // ---------------------------------------------------------------------

    /**
     * Everything worth syncing. Only *closed* sessions are included — a session still running
     * on this device is this device's business until it closes (spec §7), never something the
     * other device should see as "in progress".
     */
    /**
     * What to publish. [forceFull] republishes the whole window (the periodic reconciliation);
     * otherwise only blocks the other device has never seen, which is what keeps the payload
     * inside the Data Layer's size budget as history grows.
     *
     * Profiles and activities always travel whole -- they are a handful of rows and the other
     * side needs them to make sense of any session id.
     */
    suspend fun snapshotForSync(forceFull: Boolean = false): SyncSnapshot {
        val sinceMillis = System.currentTimeMillis() - SYNC_SESSION_WINDOW_MILLIS
        deletedSessionDao.pruneOlderThan(sinceMillis)
        val sessions = if (forceFull) {
            sessionDao.getAllSince(sinceMillis).filter { it.endTime != null }
        } else {
            sessionDao.getUnsyncedSince(sinceMillis)
        }
        return SyncSnapshot(
            payload = TrackerSyncPayload(
                profiles = profileDao.getAllOnce(),
                slices = sliceDao.getAllOnce(),
                sessions = sessions,
                deletedSessionIds = deletedSessionDao.getSince(sinceMillis).map { it.sessionId }
            ),
            sessionIds = sessions.map { it.id },
            wasFull = forceFull
        )
    }

    /** Called after a successful publish, so those rows aren't sent again. */
    suspend fun markSessionsSynced(ids: List<String>) {
        if (ids.isEmpty()) return
        sessionDao.markSynced(ids, System.currentTimeMillis())
    }

    /**
     * Merges a payload received from the other device. `isActive` is deliberately never taken
     * from the remote side — which profile is active is independent per device (you might be
     * tracking "Tri" on the watch while the phone still shows "Duo"), so we always keep the
     * local value for that one field.
     */
    suspend fun mergeSyncPayload(payload: TrackerSyncPayload) {
        payload.profiles.forEach { remote ->
            val local = profileDao.getById(remote.id)
            when {
                local == null -> profileDao.insert(remote.copy(isActive = false))
                remote.updatedAt > local.updatedAt -> profileDao.update(remote.copy(isActive = local.isActive))
            }
        }
        payload.slices.forEach { remote ->
            val local = sliceDao.getById(remote.id)
            when {
                local == null -> sliceDao.insert(remote)
                remote.updatedAt > local.updatedAt -> sliceDao.update(remote)
            }
        }
        // Deletions travel as ids: apply the remote ones locally, remember them so this device
        // passes them on, and never re-insert a session either side has already deleted.
        val remoteTombstones = payload.deletedSessionIds
        if (remoteTombstones.isNotEmpty()) {
            val now = System.currentTimeMillis()
            deletedSessionDao.insertAll(remoteTombstones.map { DeletedSessionEntity(it, now) })
            remoteTombstones.forEach { sessionDao.discard(it) }
        }

        val deleted = deletedSessionDao.getAllIds().toSet()
        payload.sessions.forEach { remote ->
            if (remote.id !in deleted) sessionDao.insertIgnore(remote)
        }
    }
}
