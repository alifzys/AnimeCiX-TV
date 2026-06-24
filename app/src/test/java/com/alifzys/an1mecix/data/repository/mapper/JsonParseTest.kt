package com.alifzys.an1mecix.data.repository.mapper

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Animecix'in tutarsız sayısal/tarih alanlarını tolere eden okuyucuların testleri. */
class JsonParseTest {

    @Test
    fun `asDouble - sayi ve sayi-string okunur`() {
        assertEquals(7.5, JsonPrimitive(7.5).asDouble()!!, 0.001)
        assertEquals(7.5, JsonPrimitive("7.5").asDouble()!!, 0.001)
    }

    @Test
    fun `asDouble - null ve sayisal olmayan null doner`() {
        assertNull(null.asDouble())
        assertNull(JsonPrimitive("abc").asDouble())
    }

    @Test
    fun `asCount - sayi okunur, dizi eleman sayisi doner`() {
        assertEquals(5, JsonPrimitive(5).asCount())
        assertEquals(3, JsonPrimitive("3").asCount())
        assertEquals(0, null.asCount())
        assertEquals(2, buildJsonArray { add(JsonPrimitive(1)); add(JsonPrimitive(2)) }.asCount())
    }

    @Test
    fun `asNonEmptyString - bos ve sifir null sayilir`() {
        assertEquals("merhaba", JsonPrimitive("merhaba").asNonEmptyString())
        assertNull(JsonPrimitive("").asNonEmptyString())
        assertNull(JsonPrimitive("0").asNonEmptyString())
        assertNull(null.asNonEmptyString())
    }

    @Test
    fun `asReleaseDate - ISO tarih Turkce aya cevrilir`() {
        assertEquals("15 Oca 2024", JsonPrimitive("2024-01-15T10:00:00Z").asReleaseDate())
        assertEquals("3 Ara 2023", JsonPrimitive("2023-12-03").asReleaseDate())
    }

    @Test
    fun `asReleaseDate - tarih yoksa null doner`() {
        assertNull(JsonPrimitive("tarih yok").asReleaseDate())
        assertNull(null.asReleaseDate())
    }
}
