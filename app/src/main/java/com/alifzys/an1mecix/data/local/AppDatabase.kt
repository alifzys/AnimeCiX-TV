package com.alifzys.an1mecix.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.alifzys.an1mecix.data.local.entities.HistoryEntry
import com.alifzys.an1mecix.data.local.entities.RatingEntry
import com.alifzys.an1mecix.data.local.entities.WatchlistEntry

@Database(
    entities = [HistoryEntry::class, WatchlistEntry::class, RatingEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun ratingDao(): RatingDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "animecix.db",
            ).build().also { INSTANCE = it }
        }
    }
}
