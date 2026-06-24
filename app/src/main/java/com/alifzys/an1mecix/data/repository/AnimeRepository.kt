package com.alifzys.an1mecix.data.repository

import com.alifzys.an1mecix.core.Constants
import com.alifzys.an1mecix.data.api.AnimeCixService
import com.alifzys.an1mecix.data.repository.mapper.asDouble
import com.alifzys.an1mecix.data.repository.mapper.asNonEmptyString
import com.alifzys.an1mecix.data.repository.mapper.extractCast
import com.alifzys.an1mecix.data.repository.mapper.groupEpisodes
import com.alifzys.an1mecix.data.repository.mapper.toCard
import com.alifzys.an1mecix.data.repository.mapper.toCommentOrNull
import com.alifzys.an1mecix.data.repository.mapper.toFeatured
import com.alifzys.an1mecix.data.repository.mapper.toLastEpisode
import com.alifzys.an1mecix.data.repository.mapper.toSeasonInfo
import com.alifzys.an1mecix.data.repository.mapper.toSource
import com.alifzys.an1mecix.domain.model.AnimeCard
import com.alifzys.an1mecix.domain.model.AnimeDetail
import com.alifzys.an1mecix.domain.model.Comment
import com.alifzys.an1mecix.domain.model.Episode
import com.alifzys.an1mecix.domain.model.FeaturedItem
import com.alifzys.an1mecix.domain.model.HomeData
import com.alifzys.an1mecix.domain.model.HomeRow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private const val FEATURED_LIST = "Sezonun İncileri"

/**
 * Animecix uçlarını çağırıp ham DTO'ları domain modellerine bağlayan orkestrasyon katmanı.
 * DTO→model dönüşümü ve parse mantığı [com.alifzys.an1mecix.data.repository.mapper] paketindedir.
 */
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
                if (sources.none { Constants.TAU_HOST in it.url }) detail
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
