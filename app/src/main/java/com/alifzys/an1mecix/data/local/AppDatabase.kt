package com.alifzys.an1mecix.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alifzys.an1mecix.data.local.entities.HistoryEntry
import com.alifzys.an1mecix.data.local.entities.RatingEntry
import com.alifzys.an1mecix.data.local.entities.SavedEpisodeEntry
import com.alifzys.an1mecix.data.local.entities.WatchlistEntry

@Database(
    entities = [HistoryEntry::class, WatchlistEntry::class, RatingEntry::class, SavedEpisodeEntry::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun ratingDao(): RatingDao
    abstract fun savedEpisodeDao(): SavedEpisodeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // v1 → v2: kaydedilen/indirilen bölümler tablosu. Mevcut history/watchlist/
        // ratings verisini koruyarak yeni tabloyu ekler.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_episodes` (
                        `episodeId` INTEGER NOT NULL,
                        `titleId` INTEGER NOT NULL,
                        `titleName` TEXT NOT NULL,
                        `titlePoster` TEXT,
                        `titleBackdrop` TEXT,
                        `seasonNumber` INTEGER NOT NULL,
                        `episodeNumber` REAL NOT NULL,
                        `episodeName` TEXT,
                        `episodePoster` TEXT,
                        `sourceUrl` TEXT NOT NULL,
                        `sourceId` INTEGER NOT NULL,
                        `quality` TEXT,
                        `filePath` TEXT,
                        `fileSize` INTEGER NOT NULL,
                        `status` INTEGER NOT NULL,
                        `progress` INTEGER NOT NULL,
                        `savedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`episodeId`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "animecix.db",
            ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
