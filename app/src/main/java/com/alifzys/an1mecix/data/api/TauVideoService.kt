package com.alifzys.an1mecix.data.api

import com.alifzys.an1mecix.core.Constants
import com.alifzys.an1mecix.domain.model.ResolvedStream
import com.alifzys.an1mecix.domain.model.SkipMarkers
import com.alifzys.an1mecix.domain.model.StreamQuality
import com.alifzys.an1mecix.domain.model.Subtitle
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request

/**
 * tau-video.xyz embed → oynatılabilir MP4 URL'leri + soft-sub altyazılar + opening/ending markörleri.
 * Animecix VideoSource'larında url = "https://tau-video.xyz/embed/{id}" geliyor.
 */
class TauVideoService {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val http = HttpClient.okHttp

    private val embedRe = Regex("""tau-video\.xyz/embed/([A-Za-z0-9]+)""")
    // animecix embed URL'i çoğu zaman "?vid=..." taşır; tau API'si altyazıları (subs)
    // yalnızca bu vid ile döndürür. vid düşerse soft-sub altyazı GELMEZ.
    private val vidParamRe = Regex("""[?&]vid=([A-Za-z0-9]+)""")
    private val referer = Constants.TAU_BASE

    // tau player HMAC anahtarı — sitenin kendi JS'inde açık (gizli değil), most-sought imzası için.
    private val playerSigKey =
        ("t4u" + "_pl4y3r_" + "s3cr3t_k3y").toByteArray(Charsets.UTF_8)

    suspend fun resolve(embedUrl: String): ResolvedStream = withContext(Dispatchers.IO) {
        val embedId = embedRe.find(embedUrl)?.groupValues?.getOrNull(1)
            ?: throw IllegalArgumentException("tau-video ID bulunamadı: $embedUrl")
        val vidParam = vidParamRe.find(embedUrl)?.groupValues?.getOrNull(1)
        val apiUrl = "${Constants.TAU_API}$embedId" + (vidParam?.let { "?vid=$it" } ?: "")

        val req = Request.Builder()
            .url(apiUrl)
            .header("Referer", referer)
            .header("Origin", referer.trimEnd('/'))
            .build()

        val dto = http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw ApiException(resp.code, "tau-video HTTP ${resp.code}")
            }
            json.decodeFromString<TauVideoDto>(resp.body?.string().orEmpty())
        }

        val qualities = (dto.urls ?: emptyList())
            .filter { it.url.isNotBlank() }
            .map { StreamQuality(label = it.label ?: "default", url = it.url, size = it.size) }
            .sortedByDescending { qualitySortKey(it.label) }

        // Soft-sub altyazılar → WebVTT endpoint'i (ExoPlayer .ass'i desteklemez, VTT'yi destekler).
        val subtitles = (dto.subs ?: emptyList()).mapNotNull { s ->
            val id = s.id?.let(::primStr) ?: return@mapNotNull null
            Subtitle(
                label = subLabel(s.language, s.name),
                language = s.language,
                url = "${Constants.TAU_VTT}$id",
            )
        }

        // Opening/ending markörleri (best-effort; başarısız olursa oynatma etkilenmez).
        val markers = runCatching { fetchMarkers(dto) }.getOrNull()

        ResolvedStream(
            provider = "tau-video",
            qualities = qualities,
            duration = dto.duration,
            thumbnails = dto.thumbnails ?: emptyMap(),
            referer = referer,
            subtitles = subtitles,
            markers = markers,
        )
    }

    /** /api/most-sought/{title}_{season}_{ep}_{translator}?tauId={_id} — x-player-sig HMAC imzalı. */
    private fun fetchMarkers(dto: TauVideoDto): SkipMarkers? {
        val titleId = dto.title_id ?: return null
        val season = dto.season_number ?: return null
        val ep = dto.episode_number?.let(::primStr) ?: return null
        val translator = dto.translator?.let(::primStr) ?: return null
        val tauId = dto.id ?: return null

        val key = "${titleId}_${season}_${ep}_${translator}"
        val req = Request.Builder()
            .url("${Constants.TAU_MOST_SOUGHT}$key?tauId=$tauId")
            .header("Referer", referer)
            .header("x-player-sig", playerSig(key))
            .build()

        return http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val m = json.decodeFromString<MostSoughtDto>(resp.body?.string().orEmpty())
            SkipMarkers(
                introFrom = m.intro?.from?.toLong(),
                introTo = m.intro?.to?.toLong(),
                outroFrom = m.outro?.from?.toLong(),
                outroTo = m.outro?.to?.toLong(),
            ).takeIf { it.introFrom != null || it.outroFrom != null }
        }
    }

    private fun playerSig(msg: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(playerSigKey, "HmacSHA256"))
        return mac.doFinal(msg.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun primStr(e: JsonElement): String? =
        runCatching { e.jsonPrimitive.content }.getOrNull()

    private fun subLabel(language: String?, name: String?): String {
        val lang = language?.takeIf { it.isNotBlank() }?.let(::langName)
        val n = name?.takeIf { it.isNotBlank() }
        return listOfNotNull(lang, n).joinToString(" - ").ifBlank { "Altyazı" }
    }

    private fun langName(code: String): String = when (code.lowercase()) {
        "tr", "tur" -> "Türkçe"
        "en", "eng" -> "İngilizce"
        "ja", "jpn" -> "Japonca"
        "de", "ger" -> "Almanca"
        "ar", "ara" -> "Arapça"
        else -> code.uppercase()
    }

    private fun qualitySortKey(label: String): Int {
        val m = Regex("""(\d+)""").find(label) ?: return 0
        return m.value.toIntOrNull() ?: 0
    }
}
