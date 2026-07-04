package com.alifzys.an1mecix.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alifzys.an1mecix.data.local.entities.HistoryEntry
import com.alifzys.an1mecix.data.local.entities.RatingEntry
import com.alifzys.an1mecix.data.local.entities.SavedEpisodeEntry
import com.alifzys.an1mecix.data.local.entities.WatchlistEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    /**
     * Devam et: her title için EN SON izlenen bölüm.
     * Aynı anime'nin 2 bölümünü izlersen, sadece son izlenen gelir.
     */
    @Query("""
        SELECT h1.* FROM history h1
        WHERE h1.progressSec / h1.durationSec < 0.95
          AND h1.updatedAt = (
            SELECT MAX(h2.updatedAt) FROM history h2 WHERE h2.titleId = h1.titleId
          )
        ORDER BY h1.updatedAt DESC
        LIMIT :limit
    """)
    fun continueWatching(limit: Int = 20): Flow<List<HistoryEntry>>

    @Query("SELECT * FROM history WHERE episodeId = :episodeId LIMIT 1")
    suspend fun forEpisode(episodeId: Int): HistoryEntry?

    @Query("SELECT * FROM history WHERE titleId = :titleId ORDER BY updatedAt DESC")
    suspend fun forTitle(titleId: Int): List<HistoryEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: HistoryEntry)

    @Query("DELETE FROM history WHERE episodeId = :episodeId")
    suspend fun delete(episodeId: Int)

    @Query("DELETE FROM history WHERE titleId = :titleId")
    suspend fun deleteByTitle(titleId: Int)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun all(): Flow<List<WatchlistEntry>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE titleId = :titleId)")
    fun isInList(titleId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: WatchlistEntry)

    @Query("DELETE FROM watchlist WHERE titleId = :titleId")
    suspend fun remove(titleId: Int)
}

@Dao
interface SavedEpisodeDao {
    @Query("SELECT * FROM saved_episodes ORDER BY savedAt DESC")
    fun all(): Flow<List<SavedEpisodeEntry>>

    /** UI'da bölüm satırının kaydet/indir durumunu canlı göstermek için. */
    @Query("SELECT episodeId, status, progress FROM saved_episodes")
    fun statuses(): Flow<List<SavedStatus>>

    @Query("SELECT * FROM saved_episodes WHERE episodeId = :episodeId LIMIT 1")
    suspend fun forEpisode(episodeId: Int): SavedEpisodeEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: SavedEpisodeEntry)

    @Query("UPDATE saved_episodes SET status = :status, progress = :progress WHERE episodeId = :episodeId")
    suspend fun updateProgress(episodeId: Int, status: Int, progress: Int)

    @Query("UPDATE saved_episodes SET status = :status, progress = :progress, filePath = :filePath, fileSize = :fileSize WHERE episodeId = :episodeId")
    suspend fun markDone(episodeId: Int, status: Int, progress: Int, filePath: String?, fileSize: Long)

    @Query("UPDATE saved_episodes SET subtitlePath = :path WHERE episodeId = :episodeId")
    suspend fun setSubtitlePath(episodeId: Int, path: String?)

    @Query("DELETE FROM saved_episodes WHERE episodeId = :episodeId")
    suspend fun delete(episodeId: Int)
}

/** Hafif projeksiyon — sadece durum izleme için. */
data class SavedStatus(
    val episodeId: Int,
    val status: Int,
    val progress: Int,
)

@Dao
interface RatingDao {
    @Query("SELECT * FROM ratings WHERE titleId = :titleId LIMIT 1")
    fun forTitle(titleId: Int): Flow<RatingEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: RatingEntry)

    @Delete
    suspend fun delete(entry: RatingEntry)
}
