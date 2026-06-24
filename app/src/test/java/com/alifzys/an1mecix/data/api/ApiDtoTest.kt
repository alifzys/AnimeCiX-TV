package com.alifzys.an1mecix.data.api

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Animecix/tau-video JSON → DTO serialization sözleşmesini sabitleyen karakterizasyon testleri.
 *
 * Amaç: Sıra 2 refaktöründe (mapper/parser ayrımı) mapper'ların dayandığı GİRİŞ şekli
 * sessizce bozulmasın. Bu testler saf JVM'de çalışır (Android bağımlılığı yok).
 */
class ApiDtoTest {

    // AnimeCixService ve TauVideoService ile aynı tolerans ayarları.
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `title parse - bilinmeyen alanlar yok sayilir, zorunlu alanlar okunur`() {
        val raw = """
            {
              "id": 42,
              "name": "Örnek Anime",
              "poster": "p.jpg",
              "year": 2021,
              "is_series": true,
              "bilinmeyen_alan": "yok sayilmali",
              "genres": [ { "id": 1, "name": "action", "display_name": "Aksiyon" } ]
            }
        """.trimIndent()

        val dto = json.decodeFromString<TitleDto>(raw)

        assertEquals(42, dto.id)
        assertEquals("Örnek Anime", dto.name)
        assertEquals(2021, dto.year)
        assertEquals(1, dto.genres?.size)
        assertEquals("Aksiyon", dto.genres?.first()?.display_name)
    }

    @Test
    fun `video parse - fansub adi extra alaninda, host name alaninda gelir`() {
        // PROJE KURALI: fansub adı `extra`'da, `name` = host (Tau Video). Bu invariant korunmalı.
        val raw = """
            {
              "id": 7,
              "name": "Tau Video",
              "url": "https://tau-video.xyz/embed/abc123",
              "extra": "AntichristHaters Fansub",
              "approved": true,
              "episode_num": 3.0,
              "season_num": 1
            }
        """.trimIndent()

        val dto = json.decodeFromString<VideoDto>(raw)

        assertEquals("Tau Video", dto.name)                 // host
        assertEquals("AntichristHaters Fansub", dto.extra)  // fansub
        assertTrue("tau-video.xyz" in dto.url)
        assertTrue(dto.approved)
        assertEquals(3.0f, dto.episode_num)
    }

    @Test
    fun `video parse - approved varsayilani true, eksik opsiyoneller null`() {
        val raw = """{ "id": 9, "url": "https://tau-video.xyz/embed/x" }"""

        val dto = json.decodeFromString<VideoDto>(raw)

        assertTrue("approved alanı yoksa varsayılan true olmalı", dto.approved)
        assertNull(dto.extra)
        assertNull(dto.name)
    }

    @Test
    fun `tau video parse - kalite listesi ve sure okunur`() {
        val raw = """
            {
              "urls": [
                { "label": "1080p", "url": "https://cdn/1080.mp4", "size": 123 },
                { "label": "720p",  "url": "https://cdn/720.mp4" }
              ],
              "thumbnails": { "0": "t0.jpg" },
              "duration": 1440.5
            }
        """.trimIndent()

        val dto = json.decodeFromString<TauVideoDto>(raw)

        assertEquals(2, dto.urls?.size)
        assertEquals("1080p", dto.urls?.first()?.label)
        assertEquals(123L, dto.urls?.first()?.size)
        assertNull("size yoksa null kalmalı", dto.urls?.get(1)?.size)
        assertEquals(1440.5, dto.duration!!, 0.001)
    }

    @Test
    fun `paginated titles parse - sayfa bilgisi ve data okunur`() {
        val raw = """
            {
              "pagination": {
                "current_page": 2,
                "last_page": 5,
                "total": 100,
                "data": [ { "id": 1, "name": "A" }, { "id": 2, "name": "B" } ]
              }
            }
        """.trimIndent()

        val dto = json.decodeFromString<PaginatedTitlesDto>(raw)

        assertNotNull(dto.pagination)
        assertEquals(2, dto.pagination?.current_page)
        assertEquals(5, dto.pagination?.last_page)
        assertEquals(2, dto.pagination?.data?.size)
    }
}
