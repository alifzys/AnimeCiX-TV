package com.alifzys.an1mecix.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bir bölümün izleme ilerlemesi — devam et listesi için.
 * Bir (titleId, episodeId) çiftine ait tek satır tutulur.
 */
@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey val episodeId: Int,
    val titleId: Int,
    val titleName: String,
    val titlePoster: String?,
    val titleBackdrop: String?,
    val seasonNumber: Int,
    val episodeNumber: Float,
    val episodeName: String?,
    val progressSec: Float,
    val durationSec: Float,
    val updatedAt: Long,
)

@Entity(tableName = "watchlist")
data class WatchlistEntry(
    @PrimaryKey val titleId: Int,
    val name: String,
    val poster: String?,
    val backdrop: String?,
    val year: Int?,
    val rating: Double?,
    val addedAt: Long,
)

@Entity(tableName = "ratings")
data class RatingEntry(
    @PrimaryKey val titleId: Int,
    val rating: Int,
    val updatedAt: Long,
)
