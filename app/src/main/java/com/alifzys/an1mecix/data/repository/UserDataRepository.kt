package com.alifzys.an1mecix.data.repository

import com.alifzys.an1mecix.data.local.HistoryDao
import com.alifzys.an1mecix.data.local.RatingDao
import com.alifzys.an1mecix.data.local.WatchlistDao
import com.alifzys.an1mecix.data.local.entities.HistoryEntry
import com.alifzys.an1mecix.data.local.entities.RatingEntry
import com.alifzys.an1mecix.data.local.entities.WatchlistEntry
import com.alifzys.an1mecix.domain.model.AnimeDetail
import com.alifzys.an1mecix.domain.model.Episode
import kotlinx.coroutines.flow.Flow

/** History + watchlist + ratings — hepsi lokal Room. Backend gerektirmez. */
class UserDataRepository(
    private val historyDao: HistoryDao,
    private val watchlistDao: WatchlistDao,
    private val ratingDao: RatingDao,
) {
    // ---- History ----
    fun continueWatching(limit: Int = 20): Flow<List<HistoryEntry>> =
        historyDao.continueWatching(limit)

    suspend fun progressFor(episodeId: Int): HistoryEntry? = historyDao.forEpisode(episodeId)

    suspend fun saveProgress(
        detail: AnimeDetail,
        episode: Episode,
        progressSec: Float,
        durationSec: Float,
    ) {
        historyDao.upsert(
            HistoryEntry(
                episodeId = episode.id,
                titleId = detail.id,
                titleName = detail.name,
                titlePoster = detail.poster,
                titleBackdrop = detail.backdrop,
                seasonNumber = episode.seasonNumber,
                episodeNumber = episode.episodeNumber,
                episodeName = episode.name,
                progressSec = progressSec,
                durationSec = durationSec,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun removeHistory(episodeId: Int) = historyDao.delete(episodeId)

    // ---- Watchlist ----
    fun watchlist(): Flow<List<WatchlistEntry>> = watchlistDao.all()
    fun isInWatchlist(titleId: Int): Flow<Boolean> = watchlistDao.isInList(titleId)

    suspend fun toggleWatchlist(detail: AnimeDetail, currentlyIn: Boolean) {
        if (currentlyIn) {
            watchlistDao.remove(detail.id)
        } else {
            watchlistDao.add(
                WatchlistEntry(
                    titleId = detail.id,
                    name = detail.name,
                    poster = detail.poster,
                    backdrop = detail.backdrop,
                    year = detail.year,
                    rating = detail.rating,
                    addedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    // ---- Ratings ----
    fun rating(titleId: Int): Flow<RatingEntry?> = ratingDao.forTitle(titleId)

    suspend fun setRating(titleId: Int, rating: Int) {
        ratingDao.upsert(RatingEntry(titleId, rating.coerceIn(1, 5), System.currentTimeMillis()))
    }
}
