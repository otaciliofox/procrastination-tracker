package com.foxlab.procrastinationtracker.trackerdata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.foxlab.procrastinationtracker.trackerdata.entity.ActivitySliceEntity
import kotlinx.coroutines.flow.Flow

data class SliceTotal(val sliceId: String, val totalMillis: Long)

@Dao
interface ActivitySliceDao {

    @Insert
    suspend fun insert(slice: ActivitySliceEntity)

    @Insert
    suspend fun insertAll(slices: List<ActivitySliceEntity>)

    @Update
    suspend fun update(slice: ActivitySliceEntity)

    @Query("UPDATE activity_slice SET archived = 1, updatedAt = :now WHERE id = :id")
    suspend fun archive(id: String, now: Long)

    @Query("SELECT * FROM activity_slice WHERE profileId = :profileId AND archived = 0 ORDER BY position")
    fun observeByProfile(profileId: String): Flow<List<ActivitySliceEntity>>

    @Query("SELECT * FROM activity_slice WHERE archived = 0 ORDER BY position")
    fun observeAllActiveSlices(): Flow<List<ActivitySliceEntity>>

    @Query("SELECT * FROM activity_slice WHERE profileId = :profileId AND archived = 0 ORDER BY position")
    suspend fun getByProfile(profileId: String): List<ActivitySliceEntity>

    @Query("SELECT * FROM activity_slice WHERE id = :id")
    suspend fun getById(id: String): ActivitySliceEntity?

    @Query("SELECT COUNT(*) FROM activity_slice WHERE profileId = :profileId AND archived = 0")
    suspend fun countInProfile(profileId: String): Int

    /** Includes archived slices too, so archival state itself syncs across devices. */
    @Query("SELECT * FROM activity_slice")
    suspend fun getAllOnce(): List<ActivitySliceEntity>

    @Query("DELETE FROM activity_slice WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)

    /**
     * Totals only count *closed* sessions (endTime IS NOT NULL). The currently running slice's
     * live elapsed time is added on top by the ViewModel, same pattern as the Timer feature.
     */
    @Query(
        """
        SELECT s.id AS sliceId,
               COALESCE(SUM(CASE WHEN se.endTime IS NOT NULL THEN se.endTime - se.startTime ELSE 0 END), 0) AS totalMillis
        FROM activity_slice s
        LEFT JOIN activity_session se ON se.sliceId = s.id AND se.startTime >= :sinceMillis
        WHERE s.profileId = :profileId AND s.archived = 0
        GROUP BY s.id
        """
    )
    fun observeTotalsSince(profileId: String, sinceMillis: Long): Flow<List<SliceTotal>>
}
