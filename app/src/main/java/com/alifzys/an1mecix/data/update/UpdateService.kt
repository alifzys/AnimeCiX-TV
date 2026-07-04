package com.alifzys.an1mecix.data.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.alifzys.an1mecix.BuildConfig
import com.alifzys.an1mecix.core.Constants
import com.alifzys.an1mecix.data.api.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File

/** GitHub Releases'ten gelen güncelleme bilgisi. */
data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val notes: String,
)

@Serializable
private data class GithubRelease(
    val tag_name: String = "",
    val body: String? = null,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
private data class GithubAsset(
    val name: String = "",
    val browser_download_url: String = "",
)

/**
 * Otomatik güncelleme: GitHub Releases API'sinden son sürümü kontrol eder,
 * yeniyse universal APK'yı indirir ve sistem kurulum akışını başlatır.
 *
 * NOT: Repo PUBLIC olmalı — private repo'da API ve asset indirme token ister,
 * token'ı açık kaynak uygulamaya gömemeyiz.
 *
 * NOT: Android, normal (sideload) bir uygulamanın sessizce kurulum yapmasına
 * izin vermez; PackageInstaller sistem onay ekranını gösterir (tek tık "Kur").
 */
object UpdateService {
    private val json = Json { ignoreUnknownKeys = true }

    /** PackageInstaller callback intent action'ı (install() ve MainActivity aynı değeri kullanır). */
    const val INSTALL_ACTION = "com.alifzys.an1mecix.INSTALL_RESULT"

    /**
     * Kurulum sonucu dinleyicisi (PackageInstaller.STATUS_*). MainActivity intent'ten iletir;
     * UpdateOverlay hata/iptal durumunda bloklamayı kaldırmak için dinler.
     */
    @Volatile
    var installStatusListener: ((Int) -> Unit)? = null

    /** Yeni sürüm varsa UpdateInfo, yoksa null döner. Hata olursa sessizce null. */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(Constants.GITHUB_RELEASES_API)
                .header("Accept", "application/vnd.github+json")
                .build()
            val body = HttpClient.okHttp.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                resp.body?.string() ?: return@withContext null
            }
            val rel = json.decodeFromString(GithubRelease.serializer(), body)
            val latest = rel.tag_name.trimStart('v', 'V').trim()
            if (latest.isEmpty() || !isNewer(latest, BuildConfig.VERSION_NAME)) {
                return@withContext null
            }
            val apk = rel.assets.firstOrNull {
                it.name.endsWith(".apk", true) && it.name.contains("universal", true)
            } ?: rel.assets.firstOrNull { it.name.endsWith(".apk", true) }
            ?: return@withContext null
            UpdateInfo(latest, apk.browser_download_url, rel.body.orEmpty())
        }.getOrNull()
    }

    /** "1.1.2" > "1.1.1" karşılaştırması (her parça sayısal). */
    @VisibleForTesting
    internal fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val l = local.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /** APK'yı indirir; onProgress 0f..1f ilerleme bildirir. */
    suspend fun download(url: String, dest: File, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url).build()
            HttpClient.okHttp.newCall(req).execute().use { resp ->
                check(resp.isSuccessful) { "indirme başarısız: ${resp.code}" }
                val rb = resp.body ?: error("boş gövde")
                val total = rb.contentLength()
                dest.outputStream().use { out ->
                    rb.byteStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buf)
                            if (read == -1) break
                            out.write(buf, 0, read)
                            downloaded += read
                            if (total > 0) onProgress((downloaded.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            dest
        }

    /** PackageInstaller ile kurulum — sistem onay ekranı çıkar (tek tık). */
    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("animecix_update", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(context, Class.forName("com.alifzys.an1mecix.MainActivity"))
                .setAction(INSTALL_ACTION)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pi = PendingIntent.getActivity(context, sessionId, intent, flags)
            session.commit(pi.intentSender)
        }
    }
}
