package com.foxlab.procrastinationtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY startTimeMillis DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startTimeMillis >= :sinceMillis ORDER BY startTimeMillis DESC")
    fun observeSince(sinceMillis: Long): Flow<List<SessionEntity>>
}
