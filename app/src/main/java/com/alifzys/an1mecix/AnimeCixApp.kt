package com.alifzys.an1mecix

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.alifzys.an1mecix.data.api.AnimeCixService
import com.alifzys.an1mecix.data.api.HttpClient
import com.alifzys.an1mecix.data.api.TauVideoService
import com.alifzys.an1mecix.data.local.AppDatabase
import com.alifzys.an1mecix.data.repository.AnimeRepository
import com.alifzys.an1mecix.data.repository.UserDataRepository

class AnimeCixApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /**
     * Global Coil ImageLoader: agresif cache, crossfade kapalı,
     * OkHttp paylaşımı (gereksiz client yaratma yok).
     * 32-bit ARM TV'de image cache hit oranı = perf.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { HttpClient.okHttp }
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.30)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(200L * 1024 * 1024)
                .build()
        }
        .crossfade(false)
        .respectCacheHeaders(false)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
}

/** Manuel DI container — Hilt/Koin gerek yok. */
class AppContainer(app: Application) {
    val api = AnimeCixService()
    val animeRepo = AnimeRepository(api)
    val tauResolver = TauVideoService()
    private val db = AppDatabase.get(app)
    val userRepo = UserDataRepository(
        historyDao = db.historyDao(),
        watchlistDao = db.watchlistDao(),
        ratingDao = db.ratingDao(),
    )
}
