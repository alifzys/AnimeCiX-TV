package com.alifzys.an1mecix.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Animecix.tv raw JSON şekilleri (sadece kullandığımız field'lar).
 * @SerialName JSON anahtarına eşler, eksik alanlar tolerans için ignoreUnknownKeys ile.
 */

@Serializable
data class TitleDto(
    val id: Int,
    val name: String? = null,
    val name_english: String? = null,
    val name_romanji: String? = null,
    val description: String? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val year: Int? = null,
    val local_vote_average: JsonElement? = null,
    val tmdb_vote_average: JsonElement? = null,
    val episode_count: Int? = null,
    val season_count: Int? = null,
    val is_series: Boolean = true,
    val runtime: Int? = null,
    val genres: List<GenreDto>? = null,
    val home_video: String? = null,
    val series_ended: Boolean = false,
    val next_episode_date: JsonElement? = null,
    val next_episode_number: Int? = null,
    val seasons: List<SeasonDto>? = null,
    val videos: List<VideoDto>? = null,
    val credits: List<CreditDto>? = null,
)

@Serializable
data class GenreDto(
    val id: Int? = null,
    val name: String? = null,
    val display_name: String? = null,
)

@Serializable
data class SeasonDto(
    val id: Int,
    val number: Int = 1,
    val name: String? = null,
    val poster: String? = null,
    val episode_count: Int = 0,
)

@Serializable
data class VideoDto(
    val id: Int,
    val name: String? = null,
    val url: String,
    val type: String? = null,
    val quality: String? = null,
    val language: String? = null,
    val extra: String? = null,   // fansub adı / çeviri kredisi burada geliyor
    val approved: Boolean = true,
    val episode_id: Int? = null,
    val episode_num: Float? = null,
    val season_num: Int? = null,
    val thumbnail: String? = null,
    val episode: EpisodeMetaDto? = null,
)

@Serializable
data class EpisodeMetaDto(
    val name: String? = null,
    val description: String? = null,
    val old_description: String? = null,
    val poster: String? = null,
    val season_number: Int? = null,
    val episode_number: Float? = null,
    val release_date: JsonElement? = null,
    val tmdb_vote_average: JsonElement? = null,
)

@Serializable
data class CreditDto(
    val id: Int,
    val name: String? = null,
    val poster: String? = null,
    val pivot: CreditPivotDto? = null,
)

@Serializable
data class CreditPivotDto(
    val department: String? = null,
    val character: String? = null,
    val job: String? = null,
    val order: Int = 0,
)

// Home endpoints

@Serializable
data class HomeListsDto(val lists: List<HomeListDto>? = null)

@Serializable
data class HomeListDto(
    val id: Int,
    val name: String? = null,
    val description: String? = null,
    val items: List<TitleDto>? = null,
)

@Serializable
data class LastEpisodesDto(val data: List<LastEpisodeDto>? = null)

@Serializable
data class LastEpisodeDto(
    val title_id: Int,
    val title_name: String? = null,
    val title_poster: String? = null,
    val season_number: Int = 1,
    val episode_number: Int = 0,
    val release_date: JsonElement? = null,
    val videos: List<LastEpisodeVideoDto>? = null,
)

@Serializable
data class LastEpisodeVideoDto(val language: String? = null)

// Detail wrapper
@Serializable
data class DetailWrapperDto(val title: TitleDto? = null)

// Related wrapper
@Serializable
data class RelatedWrapperDto(val titles: List<TitleDto>? = null)

// Search
@Serializable
data class SearchResultDto(val results: List<TitleDto>? = null)

// Videos (film / tek parça başlıklar için — detay endpoint'i video döndürmüyor)
@Serializable
data class VideosWrapperDto(val pagination: VideosPaginationDto? = null)

@Serializable
data class VideosPaginationDto(
    val current_page: Int = 1,
    val last_page: Int = 1,
    val data: List<VideoDto>? = null,
)

// Browse (categories)
@Serializable
data class PaginatedTitlesDto(val pagination: PaginationDto? = null)

@Serializable
data class PaginationDto(
    val current_page: Int = 1,
    val last_page: Int = 1,
    val total: Int = 0,
    val data: List<TitleDto>? = null,
)

// Comments
@Serializable
data class CommentsWrapperDto(val pagination: CommentsPaginationDto? = null)

@Serializable
data class CommentsPaginationDto(
    val current_page: Int = 1,
    val last_page: Int = 1,
    val total: Int = 0,
    val data: List<CommentDto>? = null,
)

@Serializable
data class CommentDto(
    @SerialName("_id") val id: String? = null,
    val user: CommentUserDto? = null,
    val content: String? = null,
    val utcTime: String? = null,
    val created_at: String? = null,
    val repliesCount: JsonElement? = null,
    val likesCount: JsonElement? = null,
    val replies: List<JsonElement>? = null,
    val likes: List<JsonElement>? = null,
    val containsSpoiler: Boolean = false,
)

@Serializable
data class CommentUserDto(
    val id: Int? = null,
    val username: String? = null,
    val display_name: String? = null,
    val avatar: String? = null,
)

// Tau-video
@Serializable
data class TauVideoDto(
    @SerialName("_id") val id: String? = null,
    val title_id: Int? = null,
    val season_number: Int? = null,
    val episode_number: JsonElement? = null,
    val translator: JsonElement? = null,
    val urls: List<TauUrlDto>? = null,
    val subs: List<TauSubDto>? = null,
    val thumbnails: Map<String, String>? = null,
    val duration: Double? = null,
)

@Serializable
data class TauUrlDto(
    val label: String? = null,
    val url: String,
    val size: Long? = null,
)

/** Soft-sub: yapay çeviri / fansub altyazı. url = ham .ass, id ile /vtt/ WebVTT alınır. */
@Serializable
data class TauSubDto(
    val id: JsonElement? = null,
    val language: String? = null,
    val name: String? = null,
    val url: String? = null,
)

/** Opening/ending markörleri (/api/most-sought). */
@Serializable
data class MostSoughtDto(
    val intro: SkipMarkerDto? = null,
    val outro: SkipMarkerDto? = null,
)

@Serializable
data class SkipMarkerDto(
    val from: Double? = null,
    val to: Double? = null,
)
