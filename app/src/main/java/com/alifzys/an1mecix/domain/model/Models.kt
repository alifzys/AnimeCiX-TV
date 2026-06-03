package com.alifzys.an1mecix.domain.model

/** UI katmanı için temizlenmiş domain modeller. */

data class AnimeCard(
    val id: Int,
    val name: String,
    val nameEnglish: String? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    val episodeCount: Int? = null,
    val seasonCount: Int? = null,
    val isSeries: Boolean = true,
)

data class FeaturedItem(
    val card: AnimeCard,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val trailerUrl: String? = null,
    val runtime: Int? = null,
)

data class HomeRow(
    val id: Int,
    val name: String,
    val items: List<AnimeCard>,
)

data class LastEpisodeItem(
    val titleId: Int,
    val titleName: String,
    val titlePoster: String?,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val hasTurkish: Boolean,
)

data class HomeData(
    val featured: List<FeaturedItem>,
    val rows: List<HomeRow>,
    val lastEpisodes: List<LastEpisodeItem>,
)

data class SeasonInfo(
    val id: Int,
    val number: Int,
    val name: String,
    val poster: String? = null,
    val episodeCount: Int = 0,
)

data class VideoSource(
    val id: Int,
    val name: String,
    val url: String,
    val type: String,
    val quality: String? = null,
    val language: String? = null,
    val fansub: String? = null,
)

data class Episode(
    val id: Int,
    val seasonNumber: Int,
    val episodeNumber: Float,
    val name: String,
    val description: String? = null,
    val poster: String? = null,
    val tmdbVoteAverage: Double? = null,
    val sources: List<VideoSource>,
)

data class CastMember(
    val id: Int,
    val name: String,
    val character: String?,
    val poster: String?,
    val order: Int = 0,
)

data class AnimeDetail(
    val id: Int,
    val name: String,
    val nameEnglish: String?,
    val description: String?,
    val poster: String?,
    val backdrop: String?,
    val year: Int?,
    val rating: Double?,
    val runtime: Int?,
    val genres: List<String>,
    val isSeries: Boolean,
    val seriesEnded: Boolean,
    val nextEpisodeDate: String?,
    val nextEpisodeNumber: Int?,
    val trailerUrl: String?,
    val seasons: List<SeasonInfo>,
    val currentSeason: Int,
    val episodes: List<Episode>,
    val cast: List<CastMember>,
    val related: List<AnimeCard>,
)

data class CommentUser(val id: Int, val username: String, val avatar: String?)

data class Comment(
    val id: String,
    val user: CommentUser?,
    val content: String,
    val createdAt: String?,
    val repliesCount: Int,
    val likesCount: Int,
    val containsSpoiler: Boolean,
)

data class StreamQuality(val label: String, val url: String, val size: Long? = null)

data class ResolvedStream(
    val provider: String,
    val qualities: List<StreamQuality>,
    val duration: Double? = null,
    /** saniye → thumbnail URL (player scrubbing için). */
    val thumbnails: Map<String, String> = emptyMap(),
    val referer: String? = null,
)

data class GenreInfo(val slug: String, val name: String, val image: String? = null)

/** Lokal kategori listesi (animecix'in dedicated endpoint'i yok). */
val GENRES: List<GenreInfo> = listOf(
    GenreInfo("action", "Aksiyon"),
    GenreInfo("adventure", "Macera"),
    GenreInfo("comedy", "Komedi"),
    GenreInfo("drama", "Dram"),
    GenreInfo("fantasy", "Fantastik"),
    GenreInfo("sci-fi", "Bilim Kurgu"),
    GenreInfo("romance", "Romantik"),
    GenreInfo("horror", "Korku"),
    GenreInfo("mystery", "Gizem"),
    GenreInfo("thriller", "Gerilim"),
    GenreInfo("animation", "Animasyon"),
    GenreInfo("slice-of-life", "Günlük Yaşam"),
    GenreInfo("supernatural", "Doğaüstü"),
    GenreInfo("sport", "Spor"),
    GenreInfo("music", "Müzikal"),
    GenreInfo("school", "Okul"),
    GenreInfo("family", "Aile"),
    GenreInfo("crime", "Suç"),
    GenreInfo("war", "Savaş"),
    GenreInfo("history", "Tarih"),
)
