package com.alifzys.an1mecix.data.api

import com.alifzys.an1mecix.domain.model.ResolvedStream
import com.alifzys.an1mecix.domain.model.StreamQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Request

/**
 * tau-video.xyz embed → oynatılabilir MP4 URL'leri.
 * Animecix VideoSource'larında url = "https://tau-video.xyz/embed/{id}" geliyor.
 */
class TauVideoService {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val http = HttpClient.okHttp

    private val embedRe = Regex("""tau-video\.xyz/embed/([A-Za-z0-9]+)""")
    private val referer = "https://tau-video.xyz/"

    suspend fun resolve(embedUrl: String): ResolvedStream = withContext(Dispatchers.IO) {
        val vid = embedRe.find(embedUrl)?.groupValues?.getOrNull(1)
            ?: throw IllegalArgumentException("tau-video ID bulunamadı: $embedUrl")

        val req = Request.Builder()
            .url("https://tau-video.xyz/api/video/$vid")
            .header("Referer", referer)
            .header("Origin", referer.trimEnd('/'))
            .build()

        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw ApiException(resp.code, "tau-video HTTP ${resp.code}")
            }
            val body = resp.body?.string().orEmpty()
            val dto = json.decodeFromString<TauVideoDto>(body)

            val qualities = (dto.urls ?: emptyList())
                .filter { it.url.isNotBlank() }
                .map { StreamQuality(label = it.label ?: "default", url = it.url, size = it.size) }
                .sortedByDescending { qualitySortKey(it.label) }

            ResolvedStream(
                provider = "tau-video",
                qualities = qualities,
                duration = dto.duration,
                thumbnails = dto.thumbnails ?: emptyMap(),
                referer = referer,
            )
        }
    }

    private fun qualitySortKey(label: String): Int {
        val m = Regex("""(\d+)""").find(label) ?: return 0
        return m.value.toIntOrNull() ?: 0
    }
}
