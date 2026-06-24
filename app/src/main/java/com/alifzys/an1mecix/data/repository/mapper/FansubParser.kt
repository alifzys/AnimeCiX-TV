package com.alifzys.an1mecix.data.repository.mapper

/**
 * `VideoDto.extra` alanından okunaklı fansub adı çıkarımı.
 *
 * AnimeCix verisi üç biçimde geliyor:
 *  1) Temiz grup adı: "AniKeyf", "PuzzleSubs", "AnimeOU Fansub"  → aynen göster
 *  2) Çok isimli liste: "Leysts - Syo", "Onderings & NightRuling" → ilk ismi al
 *  3) Rol etiketli kredi: "Çevirmen: Akira Redaktör: X Encode: Y" → çevirmeni/tekrarlayan ismi al
 * URL ve discord davetleri (çoğu rastgele kod) atılır.
 */
private val FANSUB_ROLE = Regex(
    "(çeviri\\s*[&/+]?\\s*redakte|çeviri|çeviren|çevirmen|çevirar|redakt[öo]r|redakte|redaksiyon" +
        "|edit[öo]r|encode[r]?|enkode|kodlama|upload[er]?|y[üu]kleyen|kontrol|qc|dizgi|timing" +
        "|zamanlama|karaoke|[şs]ark[ıi]|logo|tasar[ıi]m)\\s*[:\\-–]\\s*",
    RegexOption.IGNORE_CASE,
)

/** Ham `extra` metninden görünür fansub adını (max 28 karakter) çıkarır; bulunamazsa null. */
internal fun cleanFansub(raw: String): String? {
    // Discord daveti dışındaki bir fansub sitesi adresi varsa adını son çare olarak sakla
    val siteName = Regex("""(?:https?://)?(?:www\.)?([a-z0-9-]+)\.(?:com|net|org|tv|co|xyz)""", RegexOption.IGNORE_CASE)
        .find(raw)?.groupValues?.get(1)
        ?.takeIf { !it.equals("discord", true) && !it.equals("discordapp", true) }

    var s = raw
        .replace(Regex("""(https?://\S+|www\.\S+|discord(?:app)?\.(?:gg|com)/\S+|t\.me/\S+)""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\b(?:dc|discord|telegram|tg)\s*:?\s*$""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
    if (s.isBlank()) return siteName

    val name = if (FANSUB_ROLE.containsMatchIn(s)) {
        fansubFromRoles(s)
    } else {
        // Rol yok: isim listesi olabilir → ilk segmenti al ("Leysts - Syo" → "Leysts")
        s.split(Regex("""\s*[|/]\s*|\s+[-–]\s+|\s*&\s*""")).firstOrNull { it.isNotBlank() }?.trim()
    }
    return (name?.ifBlank { null } ?: siteName)?.take(28)
}

/** Rol etiketli kredi metninden tek bir görünür ad seç. */
internal fun fansubFromRoles(s: String): String? {
    val markers = FANSUB_ROLE.findAll(s).toList()
    if (markers.isEmpty()) return null
    val pairs = ArrayList<Pair<String, String>>()
    for (i in markers.indices) {
        val role = markers[i].groupValues[1].lowercase()
        val valStart = markers[i].range.last + 1
        val valEnd = if (i + 1 < markers.size) markers[i + 1].range.first else s.length
        val value = s.substring(valStart, valEnd).trim().trim(',', '/', '-', '–', '|', '.', ' ')
        if (value.isNotBlank()) pairs.add(role to value)
    }
    if (pairs.isEmpty()) return null
    // Aynı ad birden çok rolde geçiyorsa (solo çevirmen/grup) onu seç
    val repeated = pairs.groupingBy { it.second.lowercase() }.eachCount()
        .filterValues { it >= 2 }.maxByOrNull { it.value }?.key
    if (repeated != null) return pairs.first { it.second.lowercase() == repeated }.second
    // Yoksa çevirmen kredisini, o da yoksa ilkini göster
    return (pairs.firstOrNull { it.first.startsWith("çevir") } ?: pairs.first()).second
}
