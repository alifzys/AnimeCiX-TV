package com.alifzys.an1mecix.data.repository.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** `extra` → fansub adı çıkarımının davranışını sabitleyen testler. */
class FansubParserTest {

    @Test
    fun `temiz grup adi aynen korunur`() {
        assertEquals("AniKeyf", cleanFansub("AniKeyf"))
        assertEquals("AnimeOU Fansub", cleanFansub("AnimeOU Fansub"))
    }

    @Test
    fun `cok isimli listede ilk isim alinir`() {
        assertEquals("Leysts", cleanFansub("Leysts - Syo"))
        assertEquals("Onderings", cleanFansub("Onderings & NightRuling"))
    }

    @Test
    fun `rol etiketli kredide cevirmen secilir`() {
        assertEquals("Akira", cleanFansub("Çevirmen: Akira Redaktör: Mehmet Encode: Ali"))
    }

    @Test
    fun `birden cok rolde tekrar eden isim onceliklidir`() {
        // Aynı kişi hem çeviri hem redakte → o isim seçilir
        assertEquals("Solo", cleanFansub("Çeviri: Solo Redaktör: Solo Encode: Başkası"))
    }

    @Test
    fun `url ve discord daveti temizlenir, ad kalir`() {
        assertEquals("CoolSubs", cleanFansub("CoolSubs https://discord.gg/abc123"))
    }

    @Test
    fun `sadece site adresi varsa site adi son care olarak doner`() {
        assertEquals("animeler", cleanFansub("https://animeler.tv"))
    }

    @Test
    fun `discord-only icerik isim uretmez`() {
        assertNull(cleanFansub("https://discord.gg/xyz"))
    }

    @Test
    fun `28 karakter siniri uygulanir`() {
        val uzun = "A".repeat(40)
        assertEquals(28, cleanFansub(uzun)?.length)
    }
}
