package com.alifzys.an1mecix.ui.player

import android.view.KeyEvent as AKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.alifzys.an1mecix.domain.model.StreamQuality
import com.alifzys.an1mecix.domain.model.VideoSource

// ---------- Kalite seçici ----------

/** Kalite seçim sheet'i — dpad ile gezilebilir, BACK ile kapanır. */
@Composable
internal fun QualitySheet(
    qualities: List<StreamQuality>,
    current: StreamQuality,
    onSelect: (StreamQuality) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xBB000000))
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown &&
                    ev.nativeKeyEvent.keyCode == AKeyEvent.KEYCODE_BACK
                ) { onDismiss(); true } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(440.dp)
                .background(Color(0xFF15151E), RoundedCornerShape(18.dp))
                .padding(horizontal = 28.dp, vertical = 26.dp)
                .focusGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Kalite Seç",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp),
            )
            qualities.forEachIndexed { idx, q ->
                val selected = q.label == current.label
                val mod = if (idx == 0)
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).focusRequester(firstFocus)
                else
                    Modifier.fillMaxWidth().padding(vertical = 5.dp)
                Surface(
                    onClick = { onSelect(q) },
                    modifier = mod,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (selected) Color(0xFFE53935) else Color(0x22FFFFFF),
                        contentColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black,
                    ),
                ) {
                    Text(
                        text = if (selected) "● ${q.label}" else q.label,
                        fontSize = 17.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                    )
                }
            }
        }
    }
}

// ---------- Hız seçici ----------

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/** Oynatma hızı seçim sheet'i. */
@Composable
internal fun SpeedSheet(
    current: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xBB000000))
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown &&
                    ev.nativeKeyEvent.keyCode == AKeyEvent.KEYCODE_BACK
                ) { onDismiss(); true } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .background(Color(0xFF15151E), RoundedCornerShape(18.dp))
                .padding(horizontal = 28.dp, vertical = 26.dp)
                .focusGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Oynatma Hızı",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp),
            )
            SPEEDS.forEachIndexed { idx, speed ->
                val selected = speed == current
                val label = when (speed) {
                    0.5f  -> "0.5×"
                    0.75f -> "0.75×"
                    1f    -> "1× (Normal)"
                    1.25f -> "1.25×"
                    1.5f  -> "1.5×"
                    2f    -> "2×"
                    else  -> "$speed×"
                }
                val mod = if (idx == 0)
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).focusRequester(firstFocus)
                else
                    Modifier.fillMaxWidth().padding(vertical = 5.dp)
                Surface(
                    onClick = { onSelect(speed) },
                    modifier = mod,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (selected) Color(0xFFE53935) else Color(0x22FFFFFF),
                        contentColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black,
                    ),
                ) {
                    Text(
                        text = if (selected) "● $label" else label,
                        fontSize = 17.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                    )
                }
            }
        }
    }
}

// ---------- Fansub (kaynak) seçici ----------

/** Menü için tam fansub etiketi. */
private fun VideoSource.fullFansubLabel(index: Int): String {
    val f = fansub?.trim()
    if (!f.isNullOrBlank()) return f
    val lang = language?.takeIf { it.isNotBlank() }?.uppercase()
    return if (lang != null) "Kaynak ${index + 1} • $lang" else "Kaynak ${index + 1}"
}

/** Fansub (kaynak) seçim sheet'i — birden çok tau kaynağı arasından seçim. */
@Composable
internal fun FansubSheet(
    sources: List<VideoSource>,
    current: VideoSource,
    onSelect: (VideoSource) -> Unit,
    onDismiss: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xBB000000))
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown &&
                    ev.nativeKeyEvent.keyCode == AKeyEvent.KEYCODE_BACK
                ) { onDismiss(); true } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(460.dp)
                .background(Color(0xFF15151E), RoundedCornerShape(18.dp))
                .padding(horizontal = 28.dp, vertical = 26.dp)
                .focusGroup(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Fansub Seç",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp),
            )
            sources.forEachIndexed { idx, src ->
                val selected = src.id == current.id
                val label = src.fullFansubLabel(idx)
                val mod = if (idx == 0)
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).focusRequester(firstFocus)
                else
                    Modifier.fillMaxWidth().padding(vertical = 5.dp)
                Surface(
                    onClick = { onSelect(src) },
                    modifier = mod,
                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (selected) Color(0xFFE53935) else Color(0x22FFFFFF),
                        contentColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black,
                    ),
                ) {
                    Text(
                        text = if (selected) "● $label" else label,
                        fontSize = 17.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                    )
                }
            }
        }
    }
}
