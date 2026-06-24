package com.alifzys.an1mecix.data.repository.mapper

import com.alifzys.an1mecix.data.api.CommentDto
import com.alifzys.an1mecix.data.api.CommentUserDto
import com.alifzys.an1mecix.data.api.EpisodeMetaDto
import com.alifzys.an1mecix.data.api.TitleDto
import com.alifzys.an1mecix.data.api.VideoDto
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** DTO → domain model dönüşümlerinin testleri (Sıra 2 güvenlik ağının çekirdeği). */
class AnimeMapperTest {

    @Test
    fun `toCard - temel alanlar ve rating fallback`() {
        val dto = TitleDto(
            id = 1, name = "Anime A", poster = "p.jpg", year = 2020,
            local_vote_average = null, tmdb_vote_average = JsonPrimitive(8.4),
        )
        val card = dto.toCard()
        assertEquals(1, card.id)
        assertEquals("Anime A", card.name)
        assertEquals(2020, card.year)
        // local null → tmdb'ye düşer
        assertEquals(8.4, card.rating!!, 0.001)
    }

    @Test
    fun `toSource - fansub extra'dan, host name'den okunur`() {
        val dto = VideoDto(
            id = 7, name = "Tau Video", url = "https://tau-video.xyz/embed/x",
            extra = "AniKeyf", type = null,
        )
        val src = dto.toSource()
        assertEquals("Tau Video", src.name)
        assertEquals("AniKeyf", src.fansub)
        assertEquals("embed", src.type)   // type null → "embed" varsayılanı
    }

    @Test
    fun `groupEpisodes - onaysiz videolar elenir, bolume gore gruplanir ve siralanir`() {
        val videos = listOf(
            VideoDto(id = 1, url = "https://tau-video.xyz/embed/a", episode_id = 200, episode_num = 2f, season_num = 1, approved = true),
            VideoDto(id = 2, url = "https://tau-video.xyz/embed/b", episode_id = 100, episode_num = 1f, season_num = 1, approved = true),
            VideoDto(id = 3, url = "https://tau-video.xyz/embed/c", episode_id = 100, episode_num = 1f, season_num = 1, approved = true), // ep 100'e ikinci kaynak
            VideoDto(id = 4, url = "https://tau-video.xyz/embed/d", episode_id = 300, episode_num = 3f, season_num = 1, approved = false), // elenmeli
        )

        val episodes = groupEpisodes(videos)

        assertEquals("onaysız bölüm üretilmemeli", 2, episodes.size)
        // bölüm sırası: 1, 2
        assertEquals(1f, episodes[0].episodeNumber)
        assertEquals(2f, episodes[1].episodeNumber)
        // ep 100 (episodeNumber 1) iki kaynağa sahip
        assertEquals(2, episodes[0].sources.size)
    }

    @Test
    fun `groupEpisodes - isimsiz bolume varsayilan ad uretilir`() {
        val videos = listOf(
            VideoDto(id = 1, url = "https://tau-video.xyz/embed/a", episode_id = 100, episode_num = 5f, season_num = 1, episode = EpisodeMetaDto(name = null)),
        )
        val ep = groupEpisodes(videos).single()
        assertEquals("Bölüm 5", ep.name)
    }

    @Test
    fun `toCommentOrNull - bos icerik null, dolu icerik map'lenir`() {
        assertNull(CommentDto(content = null).toCommentOrNull())
        assertNull(CommentDto(content = "   ").toCommentOrNull())

        val c = CommentDto(
            id = "c1", content = "harika", containsSpoiler = true,
            user = CommentUserDto(id = 9, username = "ali"),
            repliesCount = JsonPrimitive(3),
        ).toCommentOrNull()

        assertEquals("harika", c?.content)
        assertEquals("ali", c?.user?.username)
        assertEquals(3, c?.repliesCount)
        assertTrue(c!!.containsSpoiler)
    }

    @Test
    fun `toCommentOrNull - kullanici adi yoksa anonim`() {
        val c = CommentDto(content = "x", user = CommentUserDto(id = 1)).toCommentOrNull()
        assertEquals("anonim", c?.user?.username)
    }
}
