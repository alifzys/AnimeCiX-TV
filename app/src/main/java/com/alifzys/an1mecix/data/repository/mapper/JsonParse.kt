package com.alifzys.an1mecix.data.repository.mapper

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray

/**
 * Animecix JSON alanları için tolere edici okuyucular.
 *
 * Animecix sayısal alanları bazen string, bazen null, bazen dizi gönderiyor;
 * bu yardımcılar tip belirsizliğini güvenli varsayılanlara indirger.
 */

/** Sayı veya sayı-string → Double; aksi halde null. */
internal fun JsonElement?.asDouble(): Double? {
    if (this == null) return null
    return when (this) {
        is JsonPrimitive -> doubleOrNull ?: contentOrNull?.toDoubleOrNull()
        else -> null
    }
}

/** Sayı/sayı-string → Int; dizi ise eleman sayısı; aksi halde 0. */
internal fun JsonElement?.asCount(): Int {
    if (this == null) return 0
    return when (this) {
        is JsonPrimitive -> intOrNull ?: contentOrNull?.toIntOrNull() ?: 0
        else -> try { jsonArray.size } catch (_: Exception) { 0 }
    }
}

private val TR_MONTHS = arrayOf(
    "Oca", "Şub", "Mar", "Nis", "May", "Haz",
    "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara",
)

/** Animecix release_date'i ("2024-01-15..." veya epoch) → "15 Oca 2024". */
internal fun JsonElement?.asReleaseDate(): String? {
    val raw = asNonEmptyString() ?: return null
    val m = Regex("""(\d{4})-(\d{2})-(\d{2})""").find(raw) ?: return null
    val (y, mo, d) = m.destructured
    val monthIdx = mo.toIntOrNull()?.minus(1)?.takeIf { it in 0..11 } ?: return "$d.$mo.$y"
    val day = d.toIntOrNull() ?: return "$d.$mo.$y"
    return "$day ${TR_MONTHS[monthIdx]} $y"
}

/** Boş/"0" olmayan string içeriği; aksi halde null. */
internal fun JsonElement?.asNonEmptyString(): String? {
    if (this == null) return null
    return when (this) {
        is JsonPrimitive -> {
            val s = contentOrNull ?: return null
            if (s.isBlank() || s == "0") null else s
        }
        else -> null
    }
}
