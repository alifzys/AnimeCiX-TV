package com.alifzys.an1mecix.core

/**
 * Uygulama genelindeki dış kaynak adresleri ve sabit string'ler.
 *
 * Tüm hardcoded URL/host değerleri tek noktada toplanır; bir uç adresi değişirse
 * yalnızca burası güncellenir. Site/host yapısına bağımlı olan bu değerler
 * kırılgandır (bkz. AI_Guidelines / Memory Bank teknik borç notu).
 */
object Constants {

    /** animecix.tv ana adresi (API + Referer için). */
    const val ANIMECIX_BASE = "https://animecix.tv"

    /** tau-video host'u — kaynak URL'lerinin oynatılabilir (tau) olup olmadığını ayırt etmek için kullanılır. */
    const val TAU_HOST = "tau-video.xyz"

    /** tau-video kök adresi (Referer/Origin için). */
    const val TAU_BASE = "https://tau-video.xyz/"

    /** tau-video video meta API'si; sonuna video id eklenir. */
    const val TAU_API = "https://tau-video.xyz/api/video/"

    /** tau-video altyazı (WebVTT) endpoint'i; sonuna altyazı id eklenir. */
    const val TAU_VTT = "https://tau-video.xyz/vtt/"

    /** tau-video intro/outro (opening/ending) markörleri; x-player-sig imzası ister. */
    const val TAU_MOST_SOUGHT = "https://tau-video.xyz/api/most-sought/"

    /** GitHub Releases API — en son sürüm kontrolü. */
    const val GITHUB_RELEASES_API =
        "https://api.github.com/repos/alifzys/AnimeCiX-TV/releases/latest"
}
