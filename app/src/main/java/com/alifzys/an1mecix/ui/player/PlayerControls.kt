package com.alifzys.an1mecix.ui.player

import android.view.KeyEvent as AKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.alifzys.an1mecix.domain.model.Episode

// ---------- Overlay ----------

/**
 * Oynatıcı kontrol kaplaması: üst başlık (sezon/bölüm) + alt kontrol çubuğu
 * (ilerleme, oynat/duraklat, süre, kalite/hız/fansub pill'leri, önceki/sonraki bölüm).
 * State tutmaz; tüm değerleri lambda/parametre olarak alır.
 */
@Composable
internal fun PlayerOverlay(
    title: String,
    seasonNumber: Int,
    episodeNumber: Float,
    isPlaying: () -> Boolean,
    positionMs: () -> Long,
    durationMs: () -> Long,
    qualityLabel: String,
    currentSpeed: Float,
    fansubLabel: String,
    hasMultipleSources: Boolean,
    hasSubtitles: Boolean,
    subtitleLabel: String,
    prevEp: Episode?,
    nextEp: Episode?,
    playFocus: FocusRequester,
    barFocus: FocusRequester,
    onSeekBack: () -> Unit,
    onSeekFwd: () -> Unit,
    onTogglePlay: () -> Unit,
    onHideControls: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenFansub: () -> Unit,
    onOpenSubtitle: () -> Unit,
    onPlayEpisode: (Episode) -> Unit,
) {
    val pct = androidx.compose.runtime.remember {
        androidx.compose.runtime.derivedStateOf {
            val d = durationMs(); val p = positionMs()
            if (d > 0) (p.toFloat() / d).coerceIn(0f, 1f) else 0f
        }
    }

    Box(Modifier.fillMaxSize()) {

        // Üst gradient
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.18f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xBB000000),
                        1f to Color.Transparent,
                    )
                )
        )
        // Alt gradient
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.30f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color(0xF0000000),
                    )
                )
        )

        // ── Üst sol: başlık + S1 B2 ─────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 40.dp, top = 22.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 560.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "S${seasonNumber} B${numStr(episodeNumber)}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
            )
        }

        // ── Alt kontrol çubuğu ────────────────────────────────────
        // Düzen: buton satırı ÜSTTE, ilerleme çubuğu EN ALTTA. Böylece çubuktan
        // YUKARI → butonlar, çubuktan AŞAĞI → kontrolleri gizle çalışır.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 40.dp, end = 40.dp, bottom = 20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().focusGroup(),
            ) {
                SmallPlayBtn(
                    isPlaying = isPlaying(),
                    playFocus = playFocus,
                    onTogglePlay = onTogglePlay,
                )
                Spacer(Modifier.width(12.dp))

                TimeText(positionMs = positionMs, durationMs = durationMs)
                Spacer(Modifier.width(10.dp))

                CtrlPill(text = qualityLabel, onClick = onOpenSettings)
                Spacer(Modifier.width(6.dp))

                val speedLabel = when (currentSpeed) {
                    0.5f  -> "0.5×"
                    0.75f -> "0.75×"
                    1f    -> "1×"
                    1.25f -> "1.25×"
                    1.5f  -> "1.5×"
                    2f    -> "2×"
                    else  -> "$currentSpeed×"
                }
                CtrlPill(text = speedLabel, onClick = onOpenSpeed)

                if (hasSubtitles) {
                    Spacer(Modifier.width(6.dp))
                    CtrlPill(text = "⌨ $subtitleLabel", onClick = onOpenSubtitle)
                }

                if (hasMultipleSources) {
                    Spacer(Modifier.width(6.dp))
                    CtrlPill(text = "⚑ $fansubLabel", onClick = onOpenFansub)
                }

                Spacer(Modifier.weight(1f))

                prevEp?.let { ep ->
                    CtrlPill(
                        text = "◀ B${numStr(ep.episodeNumber)}",
                        onClick = { onPlayEpisode(ep) },
                    )
                    Spacer(Modifier.width(6.dp))
                }

                nextEp?.let { ep ->
                    CtrlPill(
                        text = "B${numStr(ep.episodeNumber)} ▶",
                        onClick = { onPlayEpisode(ep) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            ProgressBar(
                pct = pct.value,
                barFocus = barFocus,
                onSeekBack = onSeekBack,
                onSeekFwd = onSeekFwd,
                onTogglePlay = onTogglePlay,
                onHideControls = onHideControls,
            )
        }
    }
}

@Composable
private fun TimeText(positionMs: () -> Long, durationMs: () -> Long) {
    Text(
        text = "${fmtTime(positionMs())} / ${fmtTime(durationMs())}",
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 14.sp,
    )
}

// ---------- Alt butonlar ----------

@Composable
private fun SmallPlayBtn(
    isPlaying: Boolean,
    playFocus: FocusRequester,
    onTogglePlay: () -> Unit,
) {
    Surface(
        onClick = onTogglePlay,
        modifier = Modifier
            .size(38.dp)
            .focusRequester(playFocus),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.6f),
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.18f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (isPlaying) "❚❚" else "▶",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CtrlPill(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0x44FFFFFF),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ProgressBar(
    pct: Float,
    barFocus: FocusRequester,
    onSeekBack: () -> Unit,
    onSeekFwd: () -> Unit,
    onTogglePlay: () -> Unit,
    onHideControls: () -> Unit,
) {
    // Birincil oynatıcı odağı: kontroller açılınca buraya odaklanılır.
    // SOL/SAĞ → ileri-geri sarma, OK → oynat/duraklat, AŞAĞI → kontrolleri gizle,
    // YUKARI → üstteki buton satırına geç (doğal odak akışı).
    var focused by remember { mutableStateOf(false) }
    val barH = if (focused) 4.dp else 2.dp
    val dotSize = if (focused) 16.dp else 10.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .focusRequester(barFocus)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (ev.nativeKeyEvent.keyCode) {
                    AKeyEvent.KEYCODE_DPAD_LEFT  -> { onSeekBack(); true }
                    AKeyEvent.KEYCODE_DPAD_RIGHT -> { onSeekFwd();  true }
                    AKeyEvent.KEYCODE_DPAD_CENTER,
                    AKeyEvent.KEYCODE_ENTER,
                    AKeyEvent.KEYCODE_NUMPAD_ENTER -> { onTogglePlay(); true }
                    AKeyEvent.KEYCODE_DPAD_DOWN -> { onHideControls(); true }
                    else -> false
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Arka plan izi
        Box(
            Modifier
                .fillMaxWidth()
                .height(barH)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = if (focused) 0.3f else 0.22f))
        )
        // Dolu kısım
        if (pct > 0.001f) {
            Box(
                Modifier
                    .fillMaxWidth(pct)
                    .height(barH)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (focused) Color(0xFFE53935) else Color.White)
            )
        }
        // Seeker dot — dolu kısmın ucunda, dikeyde ortalanmış
        Box(
            Modifier
                .fillMaxWidth(pct.coerceAtLeast(0f))
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                Modifier
                    .offset(x = (dotSize.value / 2).dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (focused) Color(0xFFE53935) else Color.White)
            )
        }
    }
}

// ---------- Yardımcılar ----------

private fun fmtTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun numStr(n: Float): String =
    if (n == n.toInt().toFloat()) n.toInt().toString() else n.toString()
