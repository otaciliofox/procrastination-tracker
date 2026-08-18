package com.foxlab.procrastinationtracker.trackerdata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.foxlab.procrastinationtracker.trackerdata.entity.LayoutProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LayoutProfileDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(profile: LayoutProfileEntity)

    @Update
    suspend fun update(profile: LayoutProfileEntity)

    @Query("DELETE FROM layout_profile WHERE id = :id AND type = 'CUSTOM'")
    suspend fun deleteCustom(id: String)

    @Query("SELECT * FROM layout_profile ORDER BY CASE type WHEN 'DUO' THEN 0 WHEN 'TRI' THEN 1 ELSE 2 END, createdAt")
    fun observeAll(): Flow<List<LayoutProfileEntity>>

    @Query("SELECT * FROM layout_profile WHERE id = :id")
    suspend fun getById(id: String): LayoutProfileEntity?

    @Query("SELECT * FROM layout_profile WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<LayoutProfileEntity?>

    @Query("SELECT COUNT(*) FROM layout_profile WHERE type = 'CUSTOM'")
    suspend fun countCustom(): Int

    @Query("SELECT title FROM layout_profile")
    suspend fun allTitles(): List<String>

    @Query("SELECT * FROM layout_profile")
    suspend fun getAllOnce(): List<LayoutProfileEntity>

    @Transaction
    suspend fun setActive(profileId: String) {
        clearActive()
        markActive(profileId)
    }

    @Query("UPDATE layout_profile SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE layout_profile SET isActive = 1 WHERE id = :profileId")
    suspend fun markActive(profileId: String)
}
