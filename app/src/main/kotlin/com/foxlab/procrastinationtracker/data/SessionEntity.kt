package com.foxlab.procrastinationtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.foxlab.procrastinationtracker.core.Phase
import com.foxlab.procrastinationtracker.core.Session
import com.foxlab.procrastinationtracker.core.TimerMode

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val phase: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val completedFully: Boolean,
    val source: String
)

fun SessionEntity.toDomain(): Session = Session(
    id = id,
    mode = TimerMode.fromName(mode),
    phase = Phase.valueOf(phase),
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    completedFully = completedFully,
    source = source
)

fun Session.toEntity(): SessionEntity = SessionEntity(
    id = id,
    mode = mode.name,
    phase = phase.name,
    startTimeMillis = startTimeMillis,
    endTimeMillis = endTimeMillis,
    completedFully = completedFully,
    source = source
)
