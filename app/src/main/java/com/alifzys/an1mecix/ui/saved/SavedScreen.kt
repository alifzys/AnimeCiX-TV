package com.alifzys.an1mecix.ui.saved

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.alifzys.an1mecix.AppContainer
import com.alifzys.an1mecix.data.local.entities.SavedEpisodeEntry

@Composable
fun SavedScreen(
    container: AppContainer,
    onPlayOffline: (episodeId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val vm: SavedViewModel = viewModel(
        factory = SavedViewModel.Factory(container.downloadManager)
    )
    val items by vm.items.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = "İndirilenler",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 56.dp, top = 28.dp, bottom = 8.dp),
            )
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Henüz indirilen bölüm yok.\nBir bölümün yanındaki ⬇ ile kaydet.",
                        color = Color(0xFF999999),
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(items, key = { it.episodeId }) { entry ->
                        SavedRow(
                            entry = entry,
                            onMainClick = {
                                when (entry.status) {
                                    SavedEpisodeEntry.STATUS_COMPLETED -> onPlayOffline(entry.episodeId)
                                    SavedEpisodeEntry.STATUS_FAILED -> vm.retry(entry.episodeId)
                                    else -> {}
                                }
                            },
                            onRemove = { vm.remove(entry.episodeId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedRow(
    entry: SavedEpisodeEntry,
    onMainClick: () -> Unit,
    onRemove: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (focused) Color(0xFF1C1C1E) else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (focused) Color.White.copy(alpha = 0.7f) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                )
                .onFocusChanged { focused = it.isFocused }
                .focusable()
                .onKeyEvent { ev ->
                    if (ev.type == KeyEventType.KeyUp &&
                        (ev.key == Key.Enter || ev.key == Key.DirectionCenter || ev.key == Key.NumPadEnter)
                    ) { onMainClick(); true } else false
                }
                .padding(10.dp),
        ) {
            Box(
                Modifier
                    .width(120.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E2A))
            ) {
                if (!entry.episodePoster.isNullOrBlank()) {
                    AsyncImage(
                        model = entry.episodePoster,
                        contentDescription = entry.titleName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.titleName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "S${entry.seasonNumber} B${numStr(entry.episodeNumber)}" +
                        (entry.episodeName?.let { " • $it" } ?: ""),
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                StatusLine(entry)
            }
        }
        Spacer(Modifier.width(8.dp))
        // Sil / iptal butonu
        androidx.tv.material3.Surface(
            onClick = onRemove,
            shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
            colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                containerColor = Color(0x22FFFFFF),
                contentColor = Color.White,
                focusedContainerColor = Color(0xFFE53935),
                focusedContentColor = Color.White,
            ),
        ) {
            Box(Modifier.height(56.dp).width(56.dp), contentAlignment = Alignment.Center) {
                Text(text = "✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatusLine(entry: SavedEpisodeEntry) {
    when (entry.status) {
        SavedEpisodeEntry.STATUS_COMPLETED -> {
            val mb = entry.fileSize / (1024.0 * 1024.0)
            Text(
                text = "✓ İndirildi" + if (mb > 1) " • ${"%.0f".format(mb)} MB" else "",
                color = Color(0xFF66BB6A),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        SavedEpisodeEntry.STATUS_DOWNLOADING -> {
            Column {
                Text(
                    text = "İndiriliyor… %${entry.progress}",
                    color = Color(0xFFE53935),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x33FFFFFF))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(entry.progress / 100f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFE53935))
                    )
                }
            }
        }
        SavedEpisodeEntry.STATUS_PENDING -> Text(
            text = "Sırada…",
            color = Color(0xFFBBBBBB),
            fontSize = 12.sp,
        )
        SavedEpisodeEntry.STATUS_FAILED -> Text(
            text = "Hata — tekrar denemek için seç",
            color = Color(0xFFFF8A80),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun numStr(n: Float): String =
    if (n == n.toInt().toFloat()) n.toInt().toString() else n.toString()
