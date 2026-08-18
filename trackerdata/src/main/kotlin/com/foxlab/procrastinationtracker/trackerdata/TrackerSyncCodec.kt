package com.foxlab.procrastinationtracker.trackerdata

import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySessionEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.LayoutProfileEntity
import com.foxlab.procrastinationtracker.trackerdata.entity.ProfileType
import com.google.android.gms.wearable.DataMap

/**
 * Encodes/decodes a [TrackerSyncPayload] to/from a Wearable [DataMap]. Kept as plain DataMap
 * (no JSON library) since play-services-wearable already gives us a nested-map-friendly type
 * that both phone and watch can read natively.
 */
object TrackerSyncCodec {

    fun encode(payload: TrackerSyncPayload): DataMap = DataMap().apply {
        putDataMapArrayList("profiles", ArrayList(payload.profiles.map { it.toDataMap() }))
        putDataMapArrayList("slices", ArrayList(payload.slices.map { it.toDataMap() }))
        putDataMapArrayList("sessions", ArrayList(payload.sessions.map { it.toDataMap() }))
        putStringArray("deletedSessionIds", payload.deletedSessionIds.toTypedArray())
    }

    fun decode(map: DataMap): TrackerSyncPayload = TrackerSyncPayload(
        profiles = map.getDataMapArrayList("profiles").orEmpty().map { it.toProfileEntity() },
        slices = map.getDataMapArrayList("slices").orEmpty().map { it.toSliceEntity() },
        sessions = map.getDataMapArrayList("sessions").orEmpty().map { it.toSessionEntity() },
        deletedSessionIds = map.getStringArray("deletedSessionIds")?.toList().orEmpty()
    )

    private fun LayoutProfileEntity.toDataMap(): DataMap = DataMap().apply {
        putString("id", id)
        putString("type", type.name)
        putString("title", title)
        putBoolean("isActive", isActive)
        forkedFromProfileId?.let { putString("forkedFromProfileId", it) }
        putLong("createdAt", createdAt)
        putLong("updatedAt", updatedAt)
    }

    private fun DataMap.toProfileEntity(): LayoutProfileEntity = LayoutProfileEntity(
        id = getString("id").orEmpty(),
        type = ProfileType.valueOf(getString("type") ?: ProfileType.CUSTOM.name),
        title = getString("title").orEmpty(),
        isActive = getBoolean("isActive"),
        forkedFromProfileId = getString("forkedFromProfileId"),
        createdAt = getLong("createdAt"),
        updatedAt = getLong("updatedAt")
    )

    private fun ActivitySliceEntity.toDataMap(): DataMap = DataMap().apply {
        putString("id", id)
        putString("profileId", profileId)
        putString("title", title)
        color?.let { putString("color", it) }
        putInt("position", position)
        timerModeId?.let { putString("timerModeId", it) }
        iconKey?.let { putString("iconKey", it) }
        putBoolean("archived", archived)
        putLong("createdAt", createdAt)
        putLong("updatedAt", updatedAt)
    }

    private fun DataMap.toSliceEntity(): ActivitySliceEntity = ActivitySliceEntity(
        id = getString("id").orEmpty(),
        profileId = getString("profileId").orEmpty(),
        title = getString("title").orEmpty(),
        color = getString("color"),
        position = getInt("position"),
        timerModeId = getString("timerModeId"),
        iconKey = getString("iconKey"),
        archived = getBoolean("archived"),
        createdAt = getLong("createdAt"),
        updatedAt = getLong("updatedAt")
    )

    private fun ActivitySessionEntity.toDataMap(): DataMap = DataMap().apply {
        putString("id", id)
        putString("sliceId", sliceId)
        subtaskId?.let { putString("subtaskId", it) }
        putLong("startTime", startTime)
        endTime?.let { putLong("endTime", it) }
        lastHeartbeatAt?.let { putLong("lastHeartbeatAt", it) }
        putString("sourceDevice", sourceDevice)
        putLong("createdAt", createdAt)
    }

    private fun DataMap.toSessionEntity(): ActivitySessionEntity = ActivitySessionEntity(
        id = getString("id").orEmpty(),
        sliceId = getString("sliceId").orEmpty(),
        subtaskId = getString("subtaskId"),
        startTime = getLong("startTime"),
        endTime = if (containsKey("endTime")) getLong("endTime") else null,
        lastHeartbeatAt = if (containsKey("lastHeartbeatAt")) getLong("lastHeartbeatAt") else null,
        sourceDevice = getString("sourceDevice").orEmpty(),
        createdAt = getLong("createdAt")
    )
}
