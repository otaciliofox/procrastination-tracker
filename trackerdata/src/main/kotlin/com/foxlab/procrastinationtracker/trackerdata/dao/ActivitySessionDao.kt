package com.foxlab.procrastinationtracker.trackerdata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivitySessionDao {

    @Insert
    suspend fun insert(session: ActivitySessionEntity)

    /** Used only when merging a sync payload: sessions are immutable once closed, so a
     * conflicting id (already known locally) simply means "nothing new here, skip it". */
    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(session: ActivitySessionEntity)

    @Query("SELECT * FROM activity_session WHERE startTime >= :sinceMillis")
    suspend fun getAllSince(sinceMillis: Long): List<ActivitySessionEntity>

    /** Closed blocks the other device has never received. */
    @Query(
        """
        SELECT * FROM activity_session
        WHERE endTime IS NOT NULL AND startTime >= :sinceMillis AND syncedAt IS NULL
        """
    )
    suspend fun getUnsyncedSince(sinceMillis: Long): List<ActivitySessionEntity>

    @Query("UPDATE activity_session SET syncedAt = :nowMillis WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, nowMillis: Long)

    @Query("UPDATE activity_session SET endTime = :endTime WHERE id = :id")
    suspend fun close(id: String, endTime: Long)

    @Query("UPDATE activity_session SET lastHeartbeatAt = :now WHERE id = :id")
    suspend fun heartbeat(id: String, now: Long)

    @Query("DELETE FROM activity_session WHERE id = :id")
    suspend fun discard(id: String)

    @Query("SELECT * FROM activity_session WHERE endTime IS NULL")
    suspend fun getAllUnclosed(): List<ActivitySessionEntity>

    /** At most one session should be running app-wide at a time (enforced by the repository). */
    @Query("SELECT * FROM activity_session WHERE endTime IS NULL LIMIT 1")
    suspend fun getRunning(): ActivitySessionEntity?

    @Query("SELECT * FROM activity_session WHERE sliceId = :sliceId ORDER BY startTime DESC")
    fun observeForSlice(sliceId: String): Flow<List<ActivitySessionEntity>>

    @Query("DELETE FROM activity_session WHERE sliceId IN (SELECT id FROM activity_slice WHERE profileId = :profileId)")
    suspend fun deleteByProfile(profileId: String)

    @Query(
        """
        SELECT id FROM activity_session
        WHERE startTime >= :sinceMillis
          AND sliceId IN (SELECT id FROM activity_slice WHERE profileId = :profileId)
        """
    )
    suspend fun getIdsForProfileSince(profileId: String, sinceMillis: Long): List<String>

    @Query("SELECT id FROM activity_session WHERE sliceId = :sliceId AND startTime >= :sinceMillis")
    suspend fun getIdsForSliceSince(sliceId: String, sinceMillis: Long): List<String>

    /** "Reiniciar a contagem de hoje" for one profile -- other profiles keep their day. */
    @Query(
        """
        DELETE FROM activity_session
        WHERE startTime >= :sinceMillis
          AND sliceId IN (SELECT id FROM activity_slice WHERE profileId = :profileId)
        """
    )
    suspend fun deleteForProfileSince(profileId: String, sinceMillis: Long)

    @Query("DELETE FROM activity_session WHERE sliceId = :sliceId AND startTime >= :sinceMillis")
    suspend fun deleteForSliceSince(sliceId: String, sinceMillis: Long)

    /** Retention: drop closed sessions older than a cutoff (the watch keeps a 30-day window). */
    @Query("DELETE FROM activity_session WHERE endTime IS NOT NULL AND startTime < :beforeMillis")
    suspend fun deleteClosedBefore(beforeMillis: Long)

    @Query(
        """
        SELECT se.id AS id, se.sliceId AS sliceId, s.title AS sliceTitle, s.profileId AS profileId, 
               se.startTime AS startTime, se.endTime AS endTime, s.iconKey AS iconKey,
               se.sourceDevice AS sourceDevice
        FROM activity_session se
        JOIN activity_slice s ON se.sliceId = s.id
        WHERE se.endTime IS NOT NULL
        ORDER BY se.startTime DESC
        """
    )
    fun observeAllClosedSessionsWithSlice(): Flow<List<SessionWithSlice>>
}

data class SessionWithSlice(
    val id: String,
    val sliceId: String,
    val sliceTitle: String,
    val profileId: String,
    val startTime: Long,
    val endTime: Long,
    val iconKey: String?,
    /** "phone" or "watch" -- which device recorded it, so reports can tell them apart. */
    val sourceDevice: String
)
