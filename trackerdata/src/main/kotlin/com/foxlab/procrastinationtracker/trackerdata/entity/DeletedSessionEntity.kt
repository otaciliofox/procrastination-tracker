package com.foxlab.procrastinationtracker.trackerdata.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A session that was deliberately removed (day reset, or a manual time correction that replaced
 * it). Sync is additive by design -- the watch records on its own and inserts into the phone --
 * so without this the other device would happily push the deleted rows straight back on the next
 * merge, and a reset would silently undo itself.
 *
 * Only the id and when it happened; the row it refers to is already gone.
 */
@Entity(tableName = "deleted_session")
data class DeletedSessionEntity(
    @PrimaryKey val sessionId: String,
    val deletedAt: Long
)
