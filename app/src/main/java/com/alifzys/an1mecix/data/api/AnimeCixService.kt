package com.alifzys.an1mecix.data.api

import com.alifzys.an1mecix.core.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import java.net.URLEncoder

/**
 * Animecix.tv ve tau-video.xyz endpoint çağrıları.
 * Sadece raw DTO döner; repository transform eder.
 */
class AnimeCixService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val http = HttpClient.okHttp

    // ---------- Helpers ----------

    /** JS encodeURIComponent eşdeğeri — query string'in X-E-H imzasıyla eşleşmesi şart. */
    private fun encURI(s: String): String =
        URLEncoder.encode(s, "UTF-8")
            .replace("+", "%20")
            .replace("%21", "!")
            .replace("%27", "'")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%7E", "~")

    private fun buildQuery(params: List<Pair<String, Any?>>): String =
        params.mapNotNull { (k, v) -> v?.let { "${encURI(k)}=${encURI(it.toString())}" } }
            .joinToString("&")

    private fun urlOf(path: String, params: List<Pair<String, Any?>> = emptyList()): String {
        val base = "${Constants.ANIMECIX_BASE}${if (path.startsWith("/")) path else "/$path"}"
        val q = buildQuery(params)
        return if (q.isEmpty()) base else "$base?$q"
    }

    private suspend inline fun <reified T> get(
        url: String,
        extraHeaders: Map<String, String> = emptyMap(),
    ): T = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .headers(extraHeaders.toHeaders())
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw ApiException(resp.code, "HTTP ${resp.code} for $url")
            }
            val body = resp.body?.string() ?: throw ApiException(resp.code, "Empty body for $url")
            json.decodeFromString<T>(body)
        }
    }

    // ---------- Home ----------

    suspend fun homeLists(): HomeListsDto =
        get(urlOf("/secure/homepage/lists-guests"))

    suspend fun lastEpisodes(page: Int = 1): LastEpisodesDto =
        get(urlOf("/secure/last-episodes", listOf("page" to page)))

    suspend fun titles(order: String = "created_at", limit: Int = 24): PaginatedTitlesDto =
        get(urlOf("/secure/titles", listOf("order" to order, "limit" to limit)))

    // ---------- Detail ----------

    suspend fun titleDetail(titleId: Int, seasonNumber: Int = 1): DetailWrapperDto =
        get(urlOf("/secure/titles/$titleId", listOf("seasonNumber" to seasonNumber)))

    suspend fun titleRelated(titleId: Int, limit: Int = 10): RelatedWrapperDto =
        get(urlOf("/secure/titles/$titleId/related", listOf("limit" to limit)))

    /**
     * Film / tek parça başlıkların videoları detay yanıtında gelmiyor
     * (show_videos=false). Ayrı /secure/videos endpoint'inden çekilir.
     */
    suspend fun videos(titleId: Int, season: Int = 1, episode: Int = 1): VideosWrapperDto =
        get(urlOf("/secure/videos", listOf(
            "titleId" to titleId,
            "season" to season,
            "episode" to episode,
        )))

    // ---------- Search ----------

    suspend fun search(query: String, limit: Int = 12): SearchResultDto =
        get(urlOf("/secure/search/${encURI(query)}", listOf(
            "type" to "undefined",
            "limit" to limit,
            "provider" to "null",
        )))

    // ---------- Browse / Categories ----------

    suspend fun browseGenre(slug: String, page: Int = 1, limit: Int = 30): PaginatedTitlesDto =
        get(urlOf("/secure/titles", listOf(
            "genre" to slug,
            "page" to page,
            "limit" to limit,
        )))

    suspend fun firstTitleOfGenre(slug: String): PaginatedTitlesDto =
        get(urlOf("/secure/titles", listOf("genre" to slug, "limit" to 1)))

    // ---------- Comments ----------

    suspend fun comments(
        titleId: Int,
        season: Int = 1,
        episode: Int = 1,
        page: Int = 1,
    ): CommentsWrapperDto {
        val key = "title_${titleId}_${season}_${episode}"
        return get(urlOf("/secure/comments/$key", listOf(
            "page" to page,
            "sortBy" to "time",
            "sortDir" to "desc",
        )))
    }
}

class ApiException(val statusCode: Int, message: String) : RuntimeException(message)
