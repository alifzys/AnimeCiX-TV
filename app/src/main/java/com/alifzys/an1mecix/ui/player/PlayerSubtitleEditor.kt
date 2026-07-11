package com.alifzys.an1mecix.ui.player

import android.view.KeyEvent as AKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.ui.CaptionStyleCompat
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.alifzys.an1mecix.R

// İç (dolgu) renk paleti — android.graphics.Color int'leri (CaptionStyleCompat ile uyumlu).
private val FILL_COLORS = listOf(
    0xFFFFFFFF.toInt(), // beyaz
    0xFFFFEB3B.toInt(), // sarı
    0xFF4FC3F7.toInt(), // açık mavi
    0xFF81C784.toInt(), // yeşil
    0xFFFFB74D.toInt(), // turuncu
    0xFFF06292.toInt(), // pembe
    0xFFE53935.toInt(), // kırmızı
    0xFF000000.toInt(), // siyah
)

// Dış (kenar/gölge) renk paleti.
private val EDGE_COLORS = listOf(
    0xFF000000.toInt(), // siyah
    0xFF333333.toInt(), // koyu gri
    0xFF0D1B3E.toInt(), // lacivert
    0xFF4A0000.toInt(), // koyu kırmızı
    0xFF2A0A3A.toInt(), // koyu mor
    0xFFFFFFFF.toInt(), // beyaz
)

private val EDGE_TYPES = listOf(
    CaptionStyleCompat.EDGE_TYPE_OUTLINE to "Kontur",
    CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW to "Gölge",
    CaptionStyleCompat.EDGE_TYPE_RAISED to "Kabartma",
    CaptionStyleCompat.EDGE_TYPE_NONE to "Kapalı",
)

/**
 * Oynatıcıdaki "Altyazı Düzenleyici" — canlı önizlemeli.
 * Boyut / dikey konum (slider), font (preset + taranan), iç & dış renk, kenar/gölge.
 * Değerler PlayerContent'te tutulur; burada sadece görüntülenip delta/seçim geri bildirilir.
 */
@Composable
internal fun SubtitleEditorSheet(
    sizeFrac: Float,
    onSizeDelta: (Int) -> Unit,
    bottomFrac: Float,
    onBottomDelta: (Int) -> Unit,
    fontId: String,
    fonts: List<SubFont>,
    onFont: (String) -> Unit,
    fillColor: Int,
    onFill: (Int) -> Unit,
    edgeColor: Int,
    onEdge: (Int) -> Unit,
    edgeType: Int,
    onEdgeType: (Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    val screenH = LocalConfiguration.current.screenHeightDp
    val panelWidth = 460.dp

    Box(
        Modifier
            .fillMaxSize()
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown &&
                    ev.nativeKeyEvent.keyCode == AKeyEvent.KEYCODE_BACK
                ) { onDismiss(); true } else false
            },
    ) {
        // ── Canlı önizleme (video üstünde, panelin solundaki alanda, gerçek konumda) ──
        Box(
            Modifier
                .fillMaxSize()
                .padding(end = panelWidth),
            contentAlignment = Alignment.BottomCenter,
        ) {
            SubtitlePreview(
                text = "Örnek altyazı — anime keyfi",
                sizeSp = (sizeFrac * screenH).sp,
                fill = Color(fillColor),
                edge = Color(edgeColor),
                edgeType = edgeType,
                family = previewFontFamily(fontId),
                modifier = Modifier.padding(
                    bottom = (bottomFrac * screenH).dp,
                    start = 24.dp,
                    end = 24.dp,
                ),
            )
        }

        // ── Sağ kontrol paneli ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(panelWidth)
                .fillMaxHeight()
                .background(Color(0xF2101018))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 28.dp)
                .focusGroup(),
        ) {
            Text(
                text = "Altyazı Düzenleyici",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Sol/sağ ile ayarla, yukarı/aşağı ile geç. Değişiklikler anında uygulanır.",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )

            SliderRow(
                label = "Boyut",
                valueLabel = "%${(sizeFrac / SubtitleStyle.DEF_SIZE * 100).toInt()}",
                fraction = norm(sizeFrac, SubtitleStyle.MIN_SIZE, SubtitleStyle.MAX_SIZE),
                focusRequester = firstFocus,
                onDelta = onSizeDelta,
            )
            Spacer(Modifier.height(14.dp))
            SliderRow(
                label = "Konum (yukarı-aşağı)",
                valueLabel = "%${(bottomFrac * 100).toInt()}",
                fraction = norm(bottomFrac, SubtitleStyle.MIN_BOTTOM, SubtitleStyle.MAX_BOTTOM),
                focusRequester = null,
                onDelta = onBottomDelta,
            )

            Spacer(Modifier.height(20.dp))
            RowLabel("Font")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fonts, key = { it.id }) { f ->
                    ChipButton(text = f.name, selected = f.id == fontId) { onFont(f.id) }
                }
            }

            Spacer(Modifier.height(20.dp))
            RowLabel("İç Renk")
            SwatchRow(colors = FILL_COLORS, selected = fillColor, onSelect = onFill)

            Spacer(Modifier.height(16.dp))
            RowLabel("Dış Renk (kenar/gölge)")
            SwatchRow(colors = EDGE_COLORS, selected = edgeColor, onSelect = onEdge)

            Spacer(Modifier.height(20.dp))
            RowLabel("Kenar / Gölge")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EDGE_TYPES.forEach { (type, name) ->
                    ChipButton(text = name, selected = type == edgeType) { onEdgeType(type) }
                }
            }

            Spacer(Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChipButton(text = "↺ Sıfırla", selected = false, onClick = onReset)
                ChipButton(text = "✓ Kapat", selected = false, onClick = onDismiss)
            }
        }
    }
}

/** 0..1 normalizasyon (bar dolgusu için). */
private fun norm(v: Float, min: Float, max: Float): Float =
    ((v - min) / (max - min)).coerceIn(0f, 1f)

@Composable
private fun RowLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFFE53935),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

/** D-pad slider satırı: SOL/SAĞ değer değiştirir, dolgu barı gösterir. */
@Composable
private fun SliderRow(
    label: String,
    valueLabel: String,
    fraction: Float,
    focusRequester: FocusRequester?,
    onDelta: (Int) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color(0x33FFFFFF) else Color(0x15FFFFFF))
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (ev.nativeKeyEvent.keyCode) {
                    AKeyEvent.KEYCODE_DPAD_LEFT -> { onDelta(-1); true }
                    AKeyEvent.KEYCODE_DPAD_RIGHT -> { onDelta(1); true }
                    else -> false
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (focused) "‹ $label ›" else label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.18f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (focused) Color(0xFFE53935) else Color.White.copy(alpha = 0.7f)),
            )
        }
    }
}

/** Renk kutucukları satırı. */
@Composable
private fun SwatchRow(colors: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { c ->
            SwatchButton(color = c, selected = c == selected) { onSelect(c) }
        }
    }
}

@Composable
private fun SwatchButton(color: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(color),
            focusedContainerColor = Color(color),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.18f),
        border = ClickableSurfaceDefaults.border(
            border = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(
                    if (selected) 3.dp else 1.dp,
                    if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                ),
                shape = CircleShape,
            ),
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (selected) {
                Text(
                    text = "✓",
                    color = if (isLight(color)) Color.Black else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ChipButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(9.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color(0xFFE53935) else Color(0x22FFFFFF),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
    ) {
        Text(
            text = if (selected) "● $text" else text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

/** CaptionStyleCompat kenar tipine göre yaklaşık canlı önizleme. */
@Composable
private fun SubtitlePreview(
    text: String,
    sizeSp: androidx.compose.ui.unit.TextUnit,
    fill: Color,
    edge: Color,
    edgeType: Int,
    family: FontFamily?,
    modifier: Modifier = Modifier,
) {
    val base = TextStyle(
        fontSize = sizeSp,
        fontWeight = FontWeight.Bold,
        fontFamily = family,
        color = fill,
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        if (edgeType == CaptionStyleCompat.EDGE_TYPE_OUTLINE) {
            val offsets = listOf(
                -2 to -2, 2 to -2, -2 to 2, 2 to 2,
                0 to -2, 0 to 2, -2 to 0, 2 to 0,
            )
            offsets.forEach { (dx, dy) ->
                Text(
                    text = text,
                    style = base.copy(color = edge),
                    modifier = Modifier.offset(dx.dp, dy.dp),
                )
            }
            Text(text = text, style = base)
        } else {
            val shadow = when (edgeType) {
                CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW -> Shadow(edge, Offset(4f, 4f), 4f)
                CaptionStyleCompat.EDGE_TYPE_RAISED -> Shadow(edge, Offset(-3f, -3f), 2f)
                else -> null
            }
            Text(text = text, style = base.copy(shadow = shadow))
        }
    }
}

/** Önizleme için font id → Compose FontFamily (preset res veya dosya). */
@Composable
private fun previewFontFamily(fontId: String): FontFamily? = when {
    fontId.startsWith("file:") ->
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            remember(fontId) {
                runCatching {
                    FontFamily(Font(java.io.File(fontId.removePrefix("file:"))))
                }.getOrNull()
            }
        } else null
    else -> {
        val res = when (fontId) {
            "quicksand" -> R.font.quicksand
            "ptsans" -> R.font.ptsans
            "ptserif" -> R.font.ptserif
            else -> R.font.amaranth
        }
        remember(res) { FontFamily(Font(res)) }
    }
}

private fun isLight(color: Int): Boolean {
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    return (0.299 * r + 0.587 * g + 0.114 * b) > 160
}

/**
 * Ayarlardan açılan TAM EKRAN altyazı düzenleyici: boş bir önizleme zemini üstünde,
 * tıpkı oynatıcıdaki gibi sağda slider'lar. Kendi state'ini tutar ve prefs'e yazar
 * (oynatmada da aynı ayarlar kullanılır). BACK ile kapanır.
 */
@Composable
internal fun SubtitleEditorScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember {
        ctx.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    }
    var sizeFrac by remember { mutableFloatStateOf(prefs.getFloat(SubtitleStyle.KEY_SIZE, SubtitleStyle.DEF_SIZE)) }
    var bottomFrac by remember { mutableFloatStateOf(prefs.getFloat(SubtitleStyle.KEY_BOTTOM, SubtitleStyle.DEF_BOTTOM)) }
    var fontId by remember { mutableStateOf(prefs.getString(SubtitleStyle.KEY_FONT, SubtitleStyle.DEF_FONT) ?: SubtitleStyle.DEF_FONT) }
    var fill by remember { mutableIntStateOf(prefs.getInt(SubtitleStyle.KEY_FILL, SubtitleStyle.DEF_FILL)) }
    var edgeColor by remember { mutableIntStateOf(prefs.getInt(SubtitleStyle.KEY_EDGE_COLOR, SubtitleStyle.DEF_EDGE_COLOR)) }
    var edgeType by remember { mutableIntStateOf(prefs.getInt(SubtitleStyle.KEY_EDGE_TYPE, SubtitleStyle.DEF_EDGE_TYPE)) }
    val fonts = remember { SubtitleStyle.availableFonts(ctx) }

    BackHandler(onBack = onBack)

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF262630), Color(0xFF121217)))
            ),
    ) {
        SubtitleEditorSheet(
            sizeFrac = sizeFrac,
            onSizeDelta = { d ->
                sizeFrac = (sizeFrac + d * SubtitleStyle.STEP_SIZE)
                    .coerceIn(SubtitleStyle.MIN_SIZE, SubtitleStyle.MAX_SIZE)
                prefs.edit().putFloat(SubtitleStyle.KEY_SIZE, sizeFrac).apply()
            },
            bottomFrac = bottomFrac,
            onBottomDelta = { d ->
                bottomFrac = (bottomFrac + d * SubtitleStyle.STEP_BOTTOM)
                    .coerceIn(SubtitleStyle.MIN_BOTTOM, SubtitleStyle.MAX_BOTTOM)
                prefs.edit().putFloat(SubtitleStyle.KEY_BOTTOM, bottomFrac).apply()
            },
            fontId = fontId,
            fonts = fonts,
            onFont = { fontId = it; prefs.edit().putString(SubtitleStyle.KEY_FONT, it).apply() },
            fillColor = fill,
            onFill = { fill = it; prefs.edit().putInt(SubtitleStyle.KEY_FILL, it).apply() },
            edgeColor = edgeColor,
            onEdge = { edgeColor = it; prefs.edit().putInt(SubtitleStyle.KEY_EDGE_COLOR, it).apply() },
            edgeType = edgeType,
            onEdgeType = { edgeType = it; prefs.edit().putInt(SubtitleStyle.KEY_EDGE_TYPE, it).apply() },
            onReset = {
                sizeFrac = SubtitleStyle.DEF_SIZE
                bottomFrac = SubtitleStyle.DEF_BOTTOM
                fontId = SubtitleStyle.DEF_FONT
                fill = SubtitleStyle.DEF_FILL
                edgeColor = SubtitleStyle.DEF_EDGE_COLOR
                edgeType = SubtitleStyle.DEF_EDGE_TYPE
                prefs.edit()
                    .putFloat(SubtitleStyle.KEY_SIZE, SubtitleStyle.DEF_SIZE)
                    .putFloat(SubtitleStyle.KEY_BOTTOM, SubtitleStyle.DEF_BOTTOM)
                    .putString(SubtitleStyle.KEY_FONT, SubtitleStyle.DEF_FONT)
                    .putInt(SubtitleStyle.KEY_FILL, SubtitleStyle.DEF_FILL)
                    .putInt(SubtitleStyle.KEY_EDGE_COLOR, SubtitleStyle.DEF_EDGE_COLOR)
                    .putInt(SubtitleStyle.KEY_EDGE_TYPE, SubtitleStyle.DEF_EDGE_TYPE)
                    .apply()
            },
            onDismiss = onBack,
        )
    }
}
