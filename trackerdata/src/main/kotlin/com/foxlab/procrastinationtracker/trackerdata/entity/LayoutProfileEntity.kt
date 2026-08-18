package com.foxlab.procrastinationtracker.trackerdata.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProfileType { DUO, TRI, CUSTOM }

/**
 * Duo and Tri are two fixed rows (id = "duo" / "tri"), seeded once and never mutated by the app.
 * Custom is 0..N rows, freely created/renamed/deleted by the user (capped at 10, enforced by
 * the repository, not the schema).
 */
@Entity(tableName = "layout_profile")
data class LayoutProfileEntity(
    @PrimaryKey val id: String,
    val type: ProfileType,
    val title: String,
    val isActive: Boolean,
    val forkedFromProfileId: String?,
    val createdAt: Long,
    val updatedAt: Long
)
