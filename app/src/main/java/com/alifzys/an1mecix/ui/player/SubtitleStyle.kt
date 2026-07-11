package com.alifzys.an1mecix.ui.player

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.media3.ui.CaptionStyleCompat
import com.alifzys.an1mecix.R
import java.io.File

/** Altyazı düzenleyicide seçilebilen font (gömülü preset veya taranan dosya). */
data class SubFont(val id: String, val name: String)

/**
 * Altyazı stil ayarları — SharedPreferences("settings") anahtarları, varsayılanları,
 * sınırları ve font çözümleme yardımcıları. Oynatıcıdaki "Altyazı Düzenleyici" bunları kullanır.
 */
object SubtitleStyle {
    const val KEY_SIZE = "sub_size_frac"        // Float: ekran yüksekliği oranı
    const val KEY_BOTTOM = "sub_bottom_frac"    // Float: alttan boşluk oranı (yukarı-aşağı konum)
    const val KEY_FONT = "sub_font"             // String: font id ("amaranth" | "file:<yol>")
    const val KEY_FILL = "sub_fill_color"       // Int: iç (dolgu) renk
    const val KEY_EDGE_COLOR = "sub_edge_color" // Int: dış (kenar/gölge) renk
    const val KEY_EDGE_TYPE = "sub_edge_type"   // Int: CaptionStyleCompat.EDGE_TYPE_*

    const val DEF_SIZE = 0.0533f
    const val DEF_BOTTOM = 0.08f
    const val DEF_FONT = "amaranth"
    const val DEF_FILL = android.graphics.Color.WHITE
    const val DEF_EDGE_COLOR = android.graphics.Color.BLACK
    const val DEF_EDGE_TYPE = CaptionStyleCompat.EDGE_TYPE_OUTLINE

    const val MIN_SIZE = 0.028f
    const val MAX_SIZE = 0.120f
    const val STEP_SIZE = 0.004f
    const val MIN_BOTTOM = 0.0f
    const val MAX_BOTTOM = 0.45f
    const val STEP_BOTTOM = 0.02f
    // Kontroller açıkken altyazıyı ilerleme çubuğunun ÜSTÜne kaldıran ek alt boşluk.
    const val CONTROLS_LIFT = 0.14f

    /** Kullanılabilir fontlar: gömülü presetler + taranan klasörlerdeki .ttf/.otf. */
    fun availableFonts(ctx: Context): List<SubFont> {
        val bundled = listOf(
            SubFont("amaranth", "Amaranth"),
            SubFont("quicksand", "Quicksand"),
            SubFont("ptsans", "PT Sans"),
            SubFont("ptserif", "PT Serif"),
        )
        val scanned = scanFontDirs(ctx)
            .map { SubFont("file:${it.absolutePath}", it.nameWithoutExtension) }
        return bundled + scanned
    }

    /** Kullanıcının .ttf atabileceği güvenilir (izin gerektirmeyen) klasör. */
    fun fontDir(ctx: Context): File? =
        ctx.getExternalFilesDir("fonts")?.also { runCatching { it.mkdirs() } }

    private fun scanFontDirs(ctx: Context): List<File> {
        val dirs = listOfNotNull(
            fontDir(ctx),
            // Bazı TV'lerde erişilebilir; erişilemezse sessizce atlanır (best-effort).
            runCatching {
                File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    ),
                    "AnimeCiX-Fonts",
                )
            }.getOrNull(),
        )
        return dirs
            .filter { runCatching { it.isDirectory }.getOrDefault(false) }
            .flatMap { d -> runCatching { d.listFiles()?.toList() }.getOrNull() ?: emptyList() }
            .filter { it.isFile && it.extension.lowercase() in setOf("ttf", "otf") }
            .distinctBy { it.name.lowercase() }
            .sortedBy { it.name.lowercase() }
    }

    /** Font id → Typeface. Gömülü preset ise res/font'tan, "file:" ise dosyadan yüklenir. */
    fun typefaceFor(ctx: Context, fontId: String): Typeface? = when {
        fontId.startsWith("file:") ->
            runCatching { Typeface.createFromFile(fontId.removePrefix("file:")) }.getOrNull()
        else -> {
            val res = when (fontId) {
                "quicksand" -> R.font.quicksand
                "ptsans" -> R.font.ptsans
                "ptserif" -> R.font.ptserif
                else -> R.font.amaranth
            }
            runCatching { ResourcesCompat.getFont(ctx, res) }.getOrNull()
        }
    }
}
