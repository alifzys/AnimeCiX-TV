package com.alifzys.an1mecix.data.api

import android.util.Base64
import java.net.URI
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Animecix X-E-H imza header'ı.
 *
 * Algoritma (backend/app/scrapers/xeh.py birebir port):
 *   plaintext = "{version}" + query_string
 *   iv        = 12 byte random
 *   key       = utf8("i4C7R2fXGocdYg" + "FLzCbDlsJ" + "jukf8G58b") -> 32 byte
 *   header    = base64(ciphertext||tag) + "." + base64(iv)
 */
object Xeh {
    const val HEADER_NAME = "X-E-H"

    private val KEY_BYTES = ("i4C7R2fXGocdYg" + "FLzCbDlsJ" + "jukf8G58b").toByteArray(Charsets.UTF_8)
    private val KEY = SecretKeySpec(KEY_BYTES, "AES")
    private val RNG = SecureRandom()

    init {
        require(KEY_BYTES.size == 32) { "X-E-H key must be 32 bytes" }
    }

    fun build(urlOrPath: String): String {
        val query = extractQuery(urlOrPath)
        val plaintext = ("{version}$query").toByteArray(Charsets.UTF_8)

        val iv = ByteArray(12).also { RNG.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, KEY, GCMParameterSpec(128, iv))
        }
        val ciphertextWithTag = cipher.doFinal(plaintext)
        val ctB64 = Base64.encodeToString(ciphertextWithTag, Base64.NO_WRAP)
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        return "$ctB64.$ivB64"
    }

    private fun extractQuery(urlOrPath: String): String {
        val qIndex = urlOrPath.indexOf('?')
        if (qIndex < 0) return ""
        return try {
            URI(urlOrPath).rawQuery ?: urlOrPath.substring(qIndex + 1)
        } catch (_: Exception) {
            urlOrPath.substring(qIndex + 1)
        }
    }
}
