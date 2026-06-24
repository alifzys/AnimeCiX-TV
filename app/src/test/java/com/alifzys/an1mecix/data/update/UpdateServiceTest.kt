package com.alifzys.an1mecix.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sürüm karşılaştırma mantığı (UpdateService.isNewer) birim testleri.
 * Saf JVM — Android bağımlılığı yok.
 */
class UpdateServiceTest {

    @Test
    fun `yeni patch surumu daha yenidir`() {
        assertTrue(UpdateService.isNewer(remote = "1.1.2", local = "1.1.1"))
    }

    @Test
    fun `ayni surum yeni degildir`() {
        assertFalse(UpdateService.isNewer(remote = "1.1.1", local = "1.1.1"))
    }

    @Test
    fun `eski surum yeni degildir`() {
        assertFalse(UpdateService.isNewer(remote = "1.0.9", local = "1.1.0"))
    }

    @Test
    fun `sayisal karsilastirma - 1_1_10, 1_1_9'dan yenidir (lexicographic degil)`() {
        assertTrue(UpdateService.isNewer(remote = "1.1.10", local = "1.1.9"))
    }

    @Test
    fun `eksik parca sifir sayilir - 1_2, 1_1_5'ten yenidir`() {
        assertTrue(UpdateService.isNewer(remote = "1.2", local = "1.1.5"))
    }

    @Test
    fun `harf iceren ekler yok sayilir - 2_0_0-beta, 1_9_9'dan yenidir`() {
        assertTrue(UpdateService.isNewer(remote = "2.0.0-beta", local = "1.9.9"))
    }
}
