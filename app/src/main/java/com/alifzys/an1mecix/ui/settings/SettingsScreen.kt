package com.alifzys.an1mecix.ui.settings

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var autoPlay by remember { mutableStateOf(prefs.getBoolean("auto_play_next", true)) }
    var skipOpening by remember { mutableStateOf(prefs.getBoolean("skip_opening", true)) }
    var skipEnding by remember { mutableStateOf(prefs.getBoolean("skip_ending", true)) }
    var defaultQuality by remember { mutableStateOf(prefs.getString("default_quality", "1080p") ?: "1080p") }
    var videoEnhance by remember {
        mutableStateOf(
            when (prefs.getString("video_enhance", "off")) {
                "sharpen" -> "Keskinlik"
                "anime4k" -> "Anime4K"
                else -> "Kapalı"
            }
        )
    }
    var anime4kScale by remember {
        mutableStateOf(
            when (prefs.getInt("anime4k_scale", 150)) {
                200 -> "2x"
                250 -> "2.5x"
                300 -> "3x"
                else -> "1.5x"
            }
        )
    }

    Row(Modifier.fillMaxSize().background(Color(0xFF0A0A0F))) {

        // Sol panel — başlık + geri butonu
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxSize()
                .padding(start = 56.dp, top = 40.dp, end = 24.dp, bottom = 40.dp),
        ) {
            Text(
                text = "AnimeCiX",
                color = Color(0xFFE53935),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Ayarlar",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 40.sp,
            )
            Spacer(Modifier.weight(1f))
            Surface(
                onClick = onBack,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color(0x22FFFFFF),
                    contentColor = Color.White.copy(alpha = 0.6f),
                    focusedContainerColor = Color.White,
                    focusedContentColor = Color.Black,
                ),
            ) {
                Text(
                    text = "← Geri",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }

        // Dikey çizgi
        Box(
            Modifier
                .width(1.dp)
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.07f))
        )

        // Sağ panel — ayarlar listesi
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 48.dp, top = 40.dp, end = 56.dp, bottom = 40.dp)
                .focusGroup(),
        ) {

            // ── OYNATMA ─────────────────────────────────────────────
            SectionLabel("OYNATMA")
            Spacer(Modifier.height(12.dp))

            ToggleRow(
                title = "Sonraki Bölümü Otomatik Oynat",
                subtitle = "Bölüm bitince sıradaki bölümü otomatik başlat",
                checked = autoPlay,
                onToggle = {
                    autoPlay = it
                    prefs.edit().putBoolean("auto_play_next", it).apply()
                },
            )

            Spacer(Modifier.height(8.dp))

            ToggleRow(
                title = "Açılışı (Opening) Otomatik Atla",
                subtitle = "Opening tespit edilince 10 sn'lik geri sayımla atlama önerilir",
                checked = skipOpening,
                onToggle = {
                    skipOpening = it
                    prefs.edit().putBoolean("skip_opening", it).apply()
                },
            )

            Spacer(Modifier.height(8.dp))

            ToggleRow(
                title = "Kapanışı (Ending) Otomatik Atla",
                subtitle = "Bölüm sonunda geri sayımla sonraki bölüme geçilir",
                checked = skipEnding,
                onToggle = {
                    skipEnding = it
                    prefs.edit().putBoolean("skip_ending", it).apply()
                },
            )

            Spacer(Modifier.height(8.dp))

            OptionRow(
                title = "Varsayılan Kalite",
                subtitle = "Yeni bir video açıldığında tercih edilecek kalite",
                options = listOf("480p", "720p", "1080p"),
                selected = defaultQuality,
                onSelect = { q ->
                    defaultQuality = q
                    prefs.edit().putString("default_quality", q).apply()
                },
            )

            Spacer(Modifier.height(32.dp))

            // ── GÖRÜNTÜ ─────────────────────────────────────────────
            SectionLabel("GÖRÜNTÜ")
            Spacer(Modifier.height(12.dp))

            OptionRow(
                title = "Görüntü İyileştirme",
                subtitle = "Keskinlik: hafif keskinleştirme (kaynak çözünürlükte). " +
                    "Anime4K: 1.5x upscale + güçlü keskinleştirme + çizgi belirginleştirme. " +
                    "Değişiklik için bölümü yeniden açın.",
                options = listOf("Kapalı", "Keskinlik", "Anime4K"),
                selected = videoEnhance,
                onSelect = { opt ->
                    videoEnhance = opt
                    val v = when (opt) {
                        "Keskinlik" -> "sharpen"
                        "Anime4K" -> "anime4k"
                        else -> "off"
                    }
                    prefs.edit().putString("video_enhance", v).apply()
                },
            )

            Spacer(Modifier.height(8.dp))

            OptionRow(
                title = "Anime4K Ölçeği",
                subtitle = "Anime4K modunda upscale oranı. Yüksek = daha net ama daha çok GPU yükü. " +
                    "Zayıf TV'de takılırsa düşür. Değişiklik için bölümü yeniden açın.",
                options = listOf("1.5x", "2x", "2.5x", "3x"),
                selected = anime4kScale,
                onSelect = { opt ->
                    anime4kScale = opt
                    val pct = when (opt) {
                        "2x" -> 200
                        "2.5x" -> 250
                        "3x" -> 300
                        else -> 150
                    }
                    prefs.edit().putInt("anime4k_scale", pct).apply()
                },
            )

            Spacer(Modifier.height(32.dp))

            // ── UYGULAMA ────────────────────────────────────────────
            SectionLabel("UYGULAMA")
            Spacer(Modifier.height(12.dp))

            InfoRow(title = "Sürüm", value = "1.1.3")
            Spacer(Modifier.height(8.dp))
            InfoRow(title = "Geliştirici", value = "Alifzys")
            Spacer(Modifier.height(8.dp))
            InfoRow(title = "Platform", value = "Android TV")
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFFE53935),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Surface(
        onClick = { onToggle(!checked) },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF15151E),
            contentColor = Color.White,
            focusedContainerColor = Color(0xFF1E1E2A),
            focusedContentColor = Color.White,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(text = subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.45f))
            }
            Spacer(Modifier.width(20.dp))
            TogglePill(checked)
        }
    }
}

@Composable
private fun TogglePill(checked: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (checked) Color(0xFFE53935) else Color(0x33FFFFFF))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (checked) "AÇIK" else "KAPALI",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
private fun OptionRow(
    title: String,
    subtitle: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF15151E))
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(Modifier.height(3.dp))
        Text(text = subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.45f))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.focusGroup()) {
            options.forEachIndexed { i, opt ->
                if (i > 0) Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = { onSelect(opt) },
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (opt == selected) Color(0xFFE53935) else Color(0x22FFFFFF),
                        contentColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black,
                    ),
                ) {
                    Text(
                        text = opt,
                        fontSize = 13.sp,
                        fontWeight = if (opt == selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF15151E))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, fontSize = 14.sp, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
