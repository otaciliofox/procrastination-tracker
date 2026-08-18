package com.foxlab.procrastinationtracker.trackerdata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.foxlab.procrastinationtracker.trackerdata.entity.DeletedSessionEntity

@Dao
interface DeletedSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tombstones: List<DeletedSessionEntity>)

    @Query("SELECT * FROM deleted_session WHERE deletedAt >= :sinceMillis")
    suspend fun getSince(sinceMillis: Long): List<DeletedSessionEntity>

    @Query("SELECT sessionId FROM deleted_session")
    suspend fun getAllIds(): List<String>

    /** Housekeeping: tombstones older than the sync window can't be needed by either device. */
    @Query("DELETE FROM deleted_session WHERE deletedAt < :beforeMillis")
    suspend fun pruneOlderThan(beforeMillis: Long)
}
