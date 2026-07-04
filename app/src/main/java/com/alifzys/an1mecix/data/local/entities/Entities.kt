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

/**
 * Kaydedilen / çevrimdışı indirilen bölüm. Tek bir [episodeId] = tek satır.
 * Hem "kaydedilenler" yer imi listesi hem indirme durumu burada tutulur.
 * [status]: 0=bekliyor, 1=iniyor, 2=tamamlandı, 3=hata.
 */
@Entity(tableName = "saved_episodes")
data class SavedEpisodeEntry(
    @PrimaryKey val episodeId: Int,
    val titleId: Int,
    val titleName: String,
    val titlePoster: String?,
    val titleBackdrop: String?,
    val seasonNumber: Int,
    val episodeNumber: Float,
    val episodeName: String?,
    val episodePoster: String?,
    /** tau-video embed URL — indirme anında mp4'e çözülür. */
    val sourceUrl: String,
    val sourceId: Int,
    val quality: String?,
    val filePath: String?,
    val fileSize: Long,
    val status: Int,
    val progress: Int,
    val savedAt: Long,
    /** İndirilen soft-sub altyazının lokal yolu (.vtt). Yoksa null. */
    val subtitlePath: String? = null,
) {
    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_DOWNLOADING = 1
        const val STATUS_COMPLETED = 2
        const val STATUS_FAILED = 3
    }
}
