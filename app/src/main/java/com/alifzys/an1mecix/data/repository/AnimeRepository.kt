package com.alifzys.an1mecix.data.repository

import com.alifzys.an1mecix.data.api.AnimeCixService
import com.alifzys.an1mecix.data.api.CommentDto
import com.alifzys.an1mecix.data.api.CreditDto
import com.alifzys.an1mecix.data.api.LastEpisodeDto
import com.alifzys.an1mecix.data.api.SeasonDto
import com.alifzys.an1mecix.data.api.TitleDto
import com.alifzys.an1mecix.data.api.VideoDto
import com.alifzys.an1mecix.domain.model.AnimeCard
import com.alifzys.an1mecix.domain.model.AnimeDetail
import com.alifzys.an1mecix.domain.model.CastMember
import com.alifzys.an1mecix.domain.model.Comment
import com.alifzys.an1mecix.domain.model.CommentUser
import com.alifzys.an1mecix.domain.model.Episode
import com.alifzys.an1mecix.domain.model.FeaturedItem
import com.alifzys.an1mecix.domain.model.HomeData
import com.alifzys.an1mecix.domain.model.HomeRow
import com.alifzys.an1mecix.domain.model.LastEpisodeItem
import com.alifzys.an1mecix.domain.model.SeasonInfo
import com.alifzys.an1mecix.domain.model.VideoSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray

private const val FEATURED_LIST = "Sezonun İncileri"

class AnimeRepository(private val api: AnimeCixService) {

    suspend fun home(): HomeData = coroutineScope {
        val listsDeferred = async { api.homeLists() }
        val lastDeferred  = async { api.lastEpisodes(page = 1) }
        val popDeferred   = async { runCatching { api.titles("most_liked", 24) }.getOrNull() }
        val newDeferred   = async { runCatching { api.titles("created_at", 24) }.getOrNull() }

        val lists = listsDeferred.await()
        val last  = lastDeferred.await()
        val pop   = popDeferred.await()
        val new   = newDeferred.await()

        var featured: List<FeaturedItem> = emptyList()
        val rows = mutableListOf<HomeRow>()
        val seenRowIds = mutableSetOf<Int>()

        for (lst in lists.lists.orEmpty()) {
            val items = lst.items.orEmpty()
            if (lst.name == FEATURED_LIST) {
                featured = items.map { it.toFeatured() }.distinctBy { it.card.id }
            } else if (seenRowIds.add(lst.id)) {
                rows += HomeRow(
                    id = lst.id,
                    name = lst.name.orEmpty(),
                    items = items.map { it.toCard() }.distinctBy { it.id },
                )
            }
        }

        // Son eklenen bölümler — zaten fetch edildi, sadece göstermiyorduk
        val lastCards = last.data.orEmpty()
            .distinctBy { it.title_id }
            .map { AnimeCard(id = it.title_id, name = it.title_name.orEmpty(), poster = it.title_poster) }
        if (lastCards.isNotEmpty()) {
            rows.add(0, HomeRow(id = -2, name = "Son Eklenen Bölümler", items = lastCards))
        }

        // Popüler animeler
        val popCards = pop?.pagination?.data.orEmpty().map { it.toCard() }.distinctBy { it.id }
        if (popCards.isNotEmpty()) {
            rows += HomeRow(id = -3, name = "Popüler Animeler", items = popCards)
        }

        // Yeni animeler
        val newCards = new?.pagination?.data.orEmpty().map { it.toCard() }.distinctBy { it.id }
        if (newCards.isNotEmpty()) {
            rows += HomeRow(id = -4, name = "Yeni Animeler", items = newCards)
        }

        HomeData(
            featured = featured,
            rows = rows,
            lastEpisodes = last.data.orEmpty().map { it.toLastEpisode() },
        )
    }

    suspend fun detail(titleId: Int, seasonNumber: Int = 1, relatedLimit: Int = 12): AnimeDetail = coroutineScope {
        val detailDeferred = async { api.titleDetail(titleId, seasonNumber) }
        // related ikincil veri — bazı başlıklarda endpoint 404 dönüyor. Patlarsa
        // tüm detayı çökertmesin diye yutuyoruz (boş "benzer" listesi).
        val relatedDeferred = async { runCatching { api.titleRelated(titleId, relatedLimit) }.getOrNull() }
        val detailResp = detailDeferred.await()
        val relatedResp = relatedDeferred.await()

        val t = detailResp.title
            ?: throw IllegalStateException("title yok: $titleId")

        AnimeDetail(
            id = t.id,
            name = t.name.orEmpty(),
            nameEnglish = t.name_english,
            description = t.description,
            poster = t.poster,
            backdrop = t.backdrop,
            year = t.year,
            rating = t.local_vote_average.asDouble() ?: t.tmdb_vote_average.asDouble(),
            runtime = t.runtime,
            genres = (t.genres ?: emptyList()).mapNotNull { it.display_name ?: it.name },
            isSeries = t.is_series,
            seriesEnded = t.series_ended,
            nextEpisodeDate = t.next_episode_date.asNonEmptyString(),
            nextEpisodeNumber = t.next_episode_number,
            trailerUrl = t.home_video,
            seasons = (t.seasons ?: emptyList()).map { it.toSeasonInfo() },
            currentSeason = seasonNumber,
            episodes = groupEpisodes(t.videos.orEmpty()),
            cast = extractCast(t.credits),
            related = relatedResp?.titles.orEmpty().map { it.toCard() }.distinctBy { it.id },
        ).let { detail ->
            // Film / tek parça: detay endpoint'i video döndürmüyor (show_videos=false,
            // videos=[]). Videolar ayrı /secure/videos?titleId&season&episode endpoint'inde.
            // Sezon/bölüm verisi olmayan başlığı season=1, episode=1 olarak çekip
            // sentetik tek bölüme sarıyoruz.
            if (detail.episodes.isEmpty()) {
                val filmVideos = runCatching { api.videos(titleId, season = 1, episode = 1) }
                    .getOrNull()?.pagination?.data.orEmpty()
                val sources = filmVideos.filter { it.approved }
                    .ifEmpty { filmVideos }
                    .map { it.toSource() }
                // En az bir oynatılabilir (tau-video) kaynak yoksa bölüm üretme.
                if (sources.none { "tau-video.xyz" in it.url }) detail
                else detail.copy(
                    episodes = listOf(
                        Episode(
                            id = detail.id,  // film için titleId'yi episodeId olarak kullan
                            seasonNumber = 1,
                            episodeNumber = 1f,
                            name = detail.name,
                            description = detail.description,
                            poster = detail.backdrop ?: detail.poster,
                            tmdbVoteAverage = detail.rating,
                            sources = sources,
                        )
                    )
                )
            } else detail
        }
    }

    suspend fun search(query: String): List<AnimeCard> {
        if (query.isBlank()) return emptyList()
        return api.search(query).results.orEmpty().map { it.toCard() }.distinctBy { it.id }
    }

    suspend fun popular(limit: Int = 24): List<AnimeCard> =
        api.titles("most_liked", limit).pagination?.data
            .orEmpty().map { it.toCard() }.distinctBy { it.id }

    suspend fun browseGenre(slug: String, page: Int = 1): Pair<List<AnimeCard>, Boolean> {
        val resp = api.browseGenre(slug, page)
        val p = resp.pagination
        val items = p?.data.orEmpty().map { it.toCard() }.distinctBy { it.id }
        val hasMore = (p?.current_page ?: page) < (p?.last_page ?: page)
        return items to hasMore
    }

    suspend fun comments(titleId: Int, season: Int, episode: Int, page: Int = 1): List<Comment> {
        val resp = api.comments(titleId, season, episode, page)
        return resp.pagination?.data.orEmpty().mapNotNull { it.toCommentOrNull() }
    }
}

// ---------- Transformers ----------

private fun TitleDto.toCard(): AnimeCard = AnimeCard(
    id = id,
    name = name.orEmpty(),
    nameEnglish = name_english,
    poster = poster,
    backdrop = backdrop,
    year = year,
    rating = local_vote_average.asDouble() ?: tmdb_vote_average.asDouble(),
    episodeCount = episode_count,
    seasonCount = season_count,
    isSeries = is_series,
)

private fun TitleDto.toFeatured(): FeaturedItem = FeaturedItem(
    card = toCard(),
    description = description,
    genres = (genres ?: emptyList()).mapNotNull { it.display_name ?: it.name },
    trailerUrl = home_video,
    runtime = runtime,
)

private fun LastEpisodeDto.toLastEpisode(): LastEpisodeItem = LastEpisodeItem(
    titleId = title_id,
    titleName = title_name.orEmpty(),
    titlePoster = title_poster,
    seasonNumber = season_number,
    episodeNumber = episode_number,
    hasTurkish = videos.orEmpty().any { it.language == "tr" },
)

private fun SeasonDto.toSeasonInfo(): SeasonInfo = SeasonInfo(
    id = id,
    number = number,
    name = name ?: "Season $number",
    poster = poster,
    episodeCount = episode_count,
)

private fun VideoDto.toSource(): VideoSource = VideoSource(
    id = id,
    name = name.orEmpty(),
    url = url,
    type = type ?: "embed",
    quality = quality,
    language = language,
    fansub = extra?.let { cleanFansub(it) },
)

/**
 * `extra` alanından okunaklı fansub adı çıkar. AnimeCix verisi üç biçimde geliyor:
 *  1) Temiz grup adı: "AniKeyf", "PuzzleSubs", "AnimeOU Fansub"  → aynen göster
 *  2) Çok isimli liste: "Leysts - Syo", "Onderings & NightRuling" → ilk ismi al
 *  3) Rol etiketli kredi: "Çevirmen: Akira Redaktör: X Encode: Y" → çevirmeni/tekrarlayan ismi al
 * URL ve discord davetleri (çoğu rastgele kod) atılır.
 */
private val FANSUB_ROLE = Regex(
    "(çeviri\\s*[&/+]?\\s*redakte|çeviri|çeviren|çevirmen|çevirar|redakt[öo]r|redakte|redaksiyon" +
        "|edit[öo]r|encode[r]?|enkode|kodlama|upload[er]?|y[üu]kleyen|kontrol|qc|dizgi|timing" +
        "|zamanlama|karaoke|[şs]ark[ıi]|logo|tasar[ıi]m)\\s*[:\\-–]\\s*",
    RegexOption.IGNORE_CASE,
)

private fun cleanFansub(raw: String): String? {
    // Discord daveti dışındaki bir fansub sitesi adresi varsa adını son çare olarak sakla
    val siteName = Regex("""(?:https?://)?(?:www\.)?([a-z0-9-]+)\.(?:com|net|org|tv|co|xyz)""", RegexOption.IGNORE_CASE)
        .find(raw)?.groupValues?.get(1)
        ?.takeIf { !it.equals("discord", true) && !it.equals("discordapp", true) }

    var s = raw
        .replace(Regex("""(https?://\S+|www\.\S+|discord(?:app)?\.(?:gg|com)/\S+|t\.me/\S+)""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\b(?:dc|discord|telegram|tg)\s*:?\s*$""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
    if (s.isBlank()) return siteName

    val name = if (FANSUB_ROLE.containsMatchIn(s)) {
        fansubFromRoles(s)
    } else {
        // Rol yok: isim listesi olabilir → ilk segmenti al ("Leysts - Syo" → "Leysts")
        s.split(Regex("""\s*[|/]\s*|\s+[-–]\s+|\s*&\s*""")).firstOrNull { it.isNotBlank() }?.trim()
    }
    return (name?.ifBlank { null } ?: siteName)?.take(28)
}

/** Rol etiketli kredi metninden tek bir görünür ad seç. */
private fun fansubFromRoles(s: String): String? {
    val markers = FANSUB_ROLE.findAll(s).toList()
    if (markers.isEmpty()) return null
    val pairs = ArrayList<Pair<String, String>>()
    for (i in markers.indices) {
        val role = markers[i].groupValues[1].lowercase()
        val valStart = markers[i].range.last + 1
        val valEnd = if (i + 1 < markers.size) markers[i + 1].range.first else s.length
        val value = s.substring(valStart, valEnd).trim().trim(',', '/', '-', '–', '|', '.', ' ')
        if (value.isNotBlank()) pairs.add(role to value)
    }
    if (pairs.isEmpty()) return null
    // Aynı ad birden çok rolde geçiyorsa (solo çevirmen/grup) onu seç
    val repeated = pairs.groupingBy { it.second.lowercase() }.eachCount()
        .filterValues { it >= 2 }.maxByOrNull { it.value }?.key
    if (repeated != null) return pairs.first { it.second.lowercase() == repeated }.second
    // Yoksa çevirmen kredisini, o da yoksa ilkini göster
    return (pairs.firstOrNull { it.first.startsWith("çevir") } ?: pairs.first()).second
}

/** Animecix /titles videos array'ini bölüme grupla — her bölüm birden çok kaynak (dil/kalite). */
private fun groupEpisodes(videos: List<VideoDto>): List<Episode> {
    val meta = LinkedHashMap<Int, EpisodeMeta>()
    val sourcesByEp = LinkedHashMap<Int, MutableList<VideoSource>>()

    for (v in videos) {
        if (!v.approved) continue
        val epId = v.episode_id ?: continue
        if (epId !in meta) {
            val em = v.episode
            meta[epId] = EpisodeMeta(
                seasonNumber = v.season_num ?: em?.season_number ?: 1,
                episodeNumber = v.episode_num ?: em?.episode_number ?: 0f,
                name = em?.name,
                description = em?.description ?: em?.old_description,
                poster = em?.poster ?: v.thumbnail,
                tmdbVoteAverage = em?.tmdb_vote_average.asDouble(),
            )
        }
        sourcesByEp.getOrPut(epId) { mutableListOf() } += v.toSource()
    }

    return meta.map { (epId, m) ->
        Episode(
            id = epId,
            seasonNumber = m.seasonNumber,
            episodeNumber = m.episodeNumber,
            name = m.name ?: "Bölüm ${formatEpisodeNumber(m.episodeNumber)}",
            description = m.description,
            poster = m.poster,
            tmdbVoteAverage = m.tmdbVoteAverage,
            sources = sourcesByEp[epId].orEmpty(),
        )
    }.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
}

private fun formatEpisodeNumber(n: Float): String =
    if (n == n.toInt().toFloat()) n.toInt().toString() else n.toString()

private data class EpisodeMeta(
    val seasonNumber: Int,
    val episodeNumber: Float,
    val name: String?,
    val description: String?,
    val poster: String?,
    val tmdbVoteAverage: Double?,
)

private fun extractCast(credits: List<CreditDto>?, limit: Int = 12): List<CastMember> {
    if (credits.isNullOrEmpty()) return emptyList()
    return credits
        .filter { it.pivot?.department == "cast" }
        .map {
            CastMember(
                id = it.id,
                name = it.name.orEmpty(),
                character = it.pivot?.character,
                poster = it.poster,
                order = it.pivot?.order ?: 0,
            )
        }
        .sortedBy { it.order }
        .take(limit)
}

private fun CommentDto.toCommentOrNull(): Comment? {
    val content = content ?: return null
    if (content.isBlank()) return null
    return Comment(
        id = id.orEmpty(),
        user = user?.let {
            CommentUser(
                id = it.id ?: 0,
                username = it.username ?: it.display_name ?: "anonim",
                avatar = it.avatar,
            )
        },
        content = content,
        createdAt = utcTime ?: created_at,
        repliesCount = repliesCount.asCount().takeIf { it > 0 } ?: replies?.size ?: 0,
        likesCount = likesCount.asCount().takeIf { it > 0 } ?: likes?.size ?: 0,
        containsSpoiler = containsSpoiler,
    )
}

// ---------- JsonElement helpers (animecix sayısal alanları bazen string/null gönderiyor) ----------

private fun JsonElement?.asDouble(): Double? {
    if (this == null) return null
    return when (this) {
        is JsonPrimitive -> doubleOrNull ?: contentOrNull?.toDoubleOrNull()
        else -> null
    }
}

private fun JsonElement?.asCount(): Int {
    if (this == null) return 0
    return when (this) {
        is JsonPrimitive -> intOrNull ?: contentOrNull?.toIntOrNull() ?: 0
        else -> try { jsonArray.size } catch (_: Exception) { 0 }
    }
}

private fun JsonElement?.asNonEmptyString(): String? {
    if (this == null) return null
    return when (this) {
        is JsonPrimitive -> {
            val s = contentOrNull ?: return null
            if (s.isBlank() || s == "0") null else s
        }
        else -> null
    }
}
