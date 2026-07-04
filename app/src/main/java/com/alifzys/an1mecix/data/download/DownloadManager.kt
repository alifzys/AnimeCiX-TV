package com.alifzys.an1mecix.data.download

import android.content.Context
import com.alifzys.an1mecix.data.api.HttpClient
import com.alifzys.an1mecix.data.api.TauVideoService
import com.alifzys.an1mecix.data.local.SavedEpisodeDao
import com.alifzys.an1mecix.data.local.SavedStatus
import com.alifzys.an1mecix.data.local.entities.SavedEpisodeEntry
import com.alifzys.an1mecix.domain.model.AnimeDetail
import com.alifzys.an1mecix.domain.model.Episode
import com.alifzys.an1mecix.domain.model.VideoSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Bölümleri TV'ye indirip çevrimdışı izlenebilir hale getirir.
 * Uygulama yaşam döngüsüne bağlı (foreground service yok) — TV genelde ön planda
 * kaldığı için yeterli. Aynı anda tek indirme (zayıf TV'yi boğmamak için).
 *
 * Dosyalar: filesDir/downloads/{episodeId}.mp4 — uygulamaya özel, izin gerekmez.
 */
class DownloadManager(
    private val appContext: Context,
    private val tau: TauVideoService,
    private val dao: SavedEpisodeDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(1)                 // aynı anda 1 indirme
    private val jobs = ConcurrentHashMap<Int, Job>()
    private val http = HttpClient.okHttp

    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

    fun saved(): Flow<List<SavedEpisodeEntry>> = dao.all()
    fun statuses(): Flow<List<SavedStatus>> = dao.statuses()
    suspend fun get(episodeId: Int): SavedEpisodeEntry? = dao.forEpisode(episodeId)

    private fun downloadsDir(): File =
        File(appContext.filesDir, "downloads").apply { mkdirs() }

    /** Bölümü kaydet + indirmeye başla. Zaten varsa hiçbir şey yapmaz. */
    fun enqueue(detail: AnimeDetail, episode: Episode, source: VideoSource) {
        if (jobs.containsKey(episode.id)) return
        val entry = SavedEpisodeEntry(
            episodeId = episode.id,
            titleId = detail.id,
            titleName = detail.name,
            titlePoster = detail.poster,
            titleBackdrop = detail.backdrop,
            seasonNumber = episode.seasonNumber,
            episodeNumber = episode.episodeNumber,
            episodeName = episode.name,
            episodePoster = episode.poster ?: detail.backdrop ?: detail.poster,
            sourceUrl = source.url,
            sourceId = source.id,
            quality = null,
            filePath = null,
            fileSize = 0,
            status = SavedEpisodeEntry.STATUS_PENDING,
            progress = 0,
            savedAt = System.currentTimeMillis(),
        )
        val job = scope.launch {
            dao.upsert(entry)
            try {
                gate.withPermit { runDownload(entry) }
            } finally {
                jobs.remove(episode.id)
            }
        }
        jobs[episode.id] = job
    }

    /** Kaydı ve indirilen dosyayı sil; sürüyorsa iptal et. */
    fun remove(episodeId: Int) {
        jobs.remove(episodeId)?.cancel()
        scope.launch {
            dao.forEpisode(episodeId)?.let { e ->
                e.filePath?.let { runCatching { File(it).delete() } }
                e.subtitlePath?.let { runCatching { File(it).delete() } }
            }
            File(downloadsDir(), "$episodeId.mp4").let { if (it.exists()) it.delete() }
            File(downloadsDir(), "$episodeId.vtt").let { if (it.exists()) it.delete() }
            dao.delete(episodeId)
        }
    }

    /** Hatalı indirmeyi yeniden dene. */
    fun retry(episodeId: Int) {
        if (jobs.containsKey(episodeId)) return
        val job = scope.launch {
            val entry = dao.forEpisode(episodeId) ?: return@launch
            try {
                gate.withPermit { runDownload(entry) }
            } finally {
                jobs.remove(episodeId)
            }
        }
        jobs[episodeId] = job
    }

    private suspend fun runDownload(entry: SavedEpisodeEntry) {
        val episodeId = entry.episodeId
        dao.updateProgress(episodeId, SavedEpisodeEntry.STATUS_DOWNLOADING, 0)
        val outFile = File(downloadsDir(), "$episodeId.mp4")
        try {
            // tau embed → mp4 kaliteleri; en yükseği indir
            val stream = tau.resolve(entry.sourceUrl)
            val quality = stream.qualities.firstOrNull()
                ?: throw IllegalStateException("İndirilebilir kalite yok")

            val req = Request.Builder()
                .url(quality.url)
                .header("User-Agent", userAgent)
                .apply { stream.referer?.let { header("Referer", it) } }
                .build()

            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                val bodyStream = resp.body?.byteStream()
                    ?: throw IllegalStateException("Boş yanıt")
                val total = resp.body?.contentLength() ?: -1L
                var downloaded = 0L
                var lastPct = -1

                outFile.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        if (!currentCoroutineContext().isActive) throw InterruptedException("İptal edildi")
                        val read = bodyStream.read(buf)
                        if (read < 0) break
                        out.write(buf, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val pct = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                            if (pct != lastPct && pct % 2 == 0) {
                                lastPct = pct
                                dao.updateProgress(
                                    episodeId, SavedEpisodeEntry.STATUS_DOWNLOADING, pct
                                )
                            }
                        }
                    }
                }
                dao.markDone(
                    episodeId,
                    SavedEpisodeEntry.STATUS_COMPLETED,
                    100,
                    outFile.absolutePath,
                    outFile.length(),
                )
            }
            // Video tamamlandıktan sonra soft-sub altyazıyı da indir (best-effort).
            runCatching { downloadSubtitle(episodeId, stream) }
        } catch (e: Exception) {
            runCatching { if (outFile.exists()) outFile.delete() }
            // İptal edilmediyse hata olarak işaretle (iptal = kayıt zaten silinecek)
            if (dao.forEpisode(episodeId) != null) {
                dao.updateProgress(episodeId, SavedEpisodeEntry.STATUS_FAILED, 0)
            }
        }
    }

    /** Türkçe (yoksa ilk) soft-sub WebVTT'yi lokal .vtt dosyasına indirip yolunu kaydeder. */
    private suspend fun downloadSubtitle(
        episodeId: Int,
        stream: com.alifzys.an1mecix.domain.model.ResolvedStream,
    ) {
        val sub = stream.subtitles.firstOrNull {
            it.language?.lowercase()?.startsWith("tr") == true
        } ?: stream.subtitles.firstOrNull() ?: return

        val subFile = File(downloadsDir(), "$episodeId.vtt")
        val req = Request.Builder()
            .url(sub.url)
            .header("User-Agent", userAgent)
            .apply { stream.referer?.let { header("Referer", it) } }
            .build()

        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return
            val bytes = resp.body?.bytes() ?: return
            subFile.writeBytes(bytes)
        }
        dao.setSubtitlePath(episodeId, subFile.absolutePath)
    }
}
