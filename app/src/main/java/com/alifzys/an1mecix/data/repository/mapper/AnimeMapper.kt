package com.alifzys.an1mecix.data.repository.mapper

import com.alifzys.an1mecix.data.api.CommentDto
import com.alifzys.an1mecix.data.api.CreditDto
import com.alifzys.an1mecix.data.api.LastEpisodeDto
import com.alifzys.an1mecix.data.api.SeasonDto
import com.alifzys.an1mecix.data.api.TitleDto
import com.alifzys.an1mecix.data.api.VideoDto
import com.alifzys.an1mecix.domain.model.AnimeCard
import com.alifzys.an1mecix.domain.model.CastMember
import com.alifzys.an1mecix.domain.model.Comment
import com.alifzys.an1mecix.domain.model.CommentUser
import com.alifzys.an1mecix.domain.model.Episode
import com.alifzys.an1mecix.domain.model.FeaturedItem
import com.alifzys.an1mecix.domain.model.LastEpisodeItem
import com.alifzys.an1mecix.domain.model.SeasonInfo
import com.alifzys.an1mecix.domain.model.VideoSource

/**
 * Animecix DTO'larını domain modellerine çeviren saf (yan etkisiz) dönüşümler.
 * Ağ/IO içermez; girdi DTO, çıktı domain modelidir → birim testi kolaydır.
 *
 * Bağımlı yardımcılar aynı paketteki [JsonParse] (asDouble/asReleaseDate...) ve
 * [FansubParser] (cleanFansub) dosyalarındadır.
 */

internal fun TitleDto.toCard(): AnimeCard = AnimeCard(
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

internal fun TitleDto.toFeatured(): FeaturedItem = FeaturedItem(
    card = toCard(),
    description = description,
    genres = (genres ?: emptyList()).mapNotNull { it.display_name ?: it.name },
    trailerUrl = home_video,
    runtime = runtime,
)

internal fun LastEpisodeDto.toLastEpisode(): LastEpisodeItem = LastEpisodeItem(
    titleId = title_id,
    titleName = title_name.orEmpty(),
    titlePoster = title_poster,
    seasonNumber = season_number,
    episodeNumber = episode_number,
    hasTurkish = videos.orEmpty().any { it.language == "tr" },
)

internal fun SeasonDto.toSeasonInfo(): SeasonInfo = SeasonInfo(
    id = id,
    number = number,
    name = name ?: "Season $number",
    poster = poster,
    episodeCount = episode_count,
)

internal fun VideoDto.toSource(): VideoSource = VideoSource(
    id = id,
    name = name.orEmpty(),
    url = url,
    type = type ?: "embed",
    quality = quality,
    language = language,
    fansub = extra?.let { cleanFansub(it) },
)

/** Animecix /titles videos array'ini bölüme grupla — her bölüm birden çok kaynak (dil/kalite). */
internal fun groupEpisodes(videos: List<VideoDto>): List<Episode> {
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
                releaseDate = em?.release_date.asReleaseDate(),
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
            releaseDate = m.releaseDate,
            sources = sourcesByEp[epId].orEmpty(),
        )
    }.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
}

internal fun formatEpisodeNumber(n: Float): String =
    if (n == n.toInt().toFloat()) n.toInt().toString() else n.toString()

private data class EpisodeMeta(
    val seasonNumber: Int,
    val episodeNumber: Float,
    val name: String?,
    val description: String?,
    val poster: String?,
    val tmdbVoteAverage: Double?,
    val releaseDate: String?,
)

internal fun extractCast(credits: List<CreditDto>?, limit: Int = 12): List<CastMember> {
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

internal fun CommentDto.toCommentOrNull(): Comment? {
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
