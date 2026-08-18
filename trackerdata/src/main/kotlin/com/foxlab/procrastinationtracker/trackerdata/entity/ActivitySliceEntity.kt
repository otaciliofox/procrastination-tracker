package com.foxlab.procrastinationtracker.trackerdata.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_slice")
data class ActivitySliceEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val title: String,
    val color: String?,
    val position: Int,
    /** NULL = free-running slice. Reserved for Fase 3 (pomodoro-per-slice); unused in Fase 1. */
    val timerModeId: String?,
    val iconKey: String? = null,
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
