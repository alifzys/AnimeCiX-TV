package com.alifzys.an1mecix.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
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
import com.alifzys.an1mecix.domain.model.AnimeDetail
import com.alifzys.an1mecix.domain.model.Episode
import com.alifzys.an1mecix.domain.model.SeasonInfo
import com.alifzys.an1mecix.domain.model.VideoSource
import com.alifzys.an1mecix.ui.components.FullScreenLoading
import com.alifzys.an1mecix.ui.components.LoadingBar

@Composable
fun DetailScreen(
    container: AppContainer,
    titleId: Int,
    onPlayEpisode: (titleId: Int, seasonNumber: Int, episodeId: Int, sourceId: Int) -> Unit,
    onBack: () -> Unit,
) {
    val vm: DetailViewModel = viewModel(
        key = "detail-$titleId",
        factory = DetailViewModel.Factory(
            titleId, container.animeRepo, container.userRepo, container.tauResolver
        )
    )
    val state by vm.state.collectAsStateWithLifecycle()

    // Geri tuşu her zaman çalışsın (focus durumundan bağımsız)
    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val s = state) {
            DetailUiState.Loading -> FullScreenLoading()
            is DetailUiState.Error -> CenterText("Hata: ${s.message}")
            is DetailUiState.Ready -> DetailContent(
                detail = s.detail,
                inWatchlist = s.inWatchlist,
                seasonLoading = s.seasonLoading,
                onToggleWatchlist = vm::toggleWatchlist,
                onSeasonSelected = { vm.selectSeason(it.number) },
                onPlay = { ep, sourceId -> onPlayEpisode(s.detail.id, ep.seasonNumber, ep.id, sourceId) },
            )
        }
    }
}

@Composable
private fun DetailContent(
    detail: AnimeDetail,
    inWatchlist: Boolean,
    seasonLoading: Boolean,
    onToggleWatchlist: () -> Unit,
    onSeasonSelected: (SeasonInfo) -> Unit,
    onPlay: (Episode, Int) -> Unit,
) {
    // Tıklanan bölüm için fansub seçim ekranı (birden çok kaynak varsa)
    var fansubFor by remember { mutableStateOf<Episode?>(null) }

    // Plain scrollable Column: LazyColumn'da focus item'lar görünmezleşince
    // dispose oluyor ve yukarı/aşağı focus geçişi bozuluyor. Anime için bölüm
    // sayısı (genelde <100) plain Column'u kaldırır.
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    // Üstteki ilk buton ("Listeme Ekle") — bölümlerden yukarı çıkışta buraya dönülür.
    val topFocus = remember { FocusRequester() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(bottom = 32.dp),
    ) {
        Backdrop(detail)
        Spacer(Modifier.height(16.dp))
        DetailMeta(
            detail = detail,
            inWatchlist = inWatchlist,
            onToggleWatchlist = onToggleWatchlist,
            topFocus = topFocus,
            onTopFocused = { scope.launch { scroll.animateScrollTo(0) } },
        )
        if (detail.seasons.size > 1) {
            Spacer(Modifier.height(20.dp))
            SeasonSelector(seasons = detail.seasons, current = detail.currentSeason, onSelect = onSeasonSelected)
        }
        Spacer(Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 48.dp, bottom = 10.dp),
        ) {
            val isFilm = !detail.isSeries ||
                (detail.episodes.size == 1 && detail.episodes.firstOrNull()?.id == detail.id)
            Text(
                text = if (isFilm) "Film" else "Bölümler (${detail.episodes.size})",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (seasonLoading) {
                Spacer(Modifier.width(16.dp))
                LoadingBar(modifier = Modifier.width(120.dp))
            }
        }
        // Sezon yüklenirken eski bölümleri soluklaştır ama ekranı boşaltma
        Column(Modifier.alpha(if (seasonLoading) 0.35f else 1f)) {
            detail.episodes.forEachIndexed { index, ep ->
                EpisodeRow(
                    ep,
                    onPlay = {
                        val playable = ep.playableSources()
                        if (playable.size > 1) fansubFor = ep
                        else onPlay(ep, playable.firstOrNull()?.id ?: -1)
                    },
                    // İlk bölümden YUKARI → focus'u deterministik olarak üstteki butona
                    // yönlendir (focusProperties). Buton artık ekranın üst kısmında (bilgilerin
                    // hemen altında) olduğu için focus oraya gidince backdrop fotoğrafı da
                    // görünür kalıyor; onFocusChanged en üste (0) kaydırıyor.
                    focusUp = if (index == 0) topFocus else null,
                )
            }
        }
    }

    // Fansub seçim overlay'i
    fansubFor?.let { ep ->
        FansubSheet(
            episode = ep,
            onSelect = { source ->
                fansubFor = null
                onPlay(ep, source.id)
            },
            onDismiss = { fansubFor = null },
        )
    }
}

/** Bu bölüm için oynatılabilir (tau-video) kaynaklar = fansublar. */
private fun Episode.playableSources(): List<VideoSource> =
    sources.filter { "tau-video.xyz" in it.url }

private fun VideoSource.fansubLabel(index: Int): String {
    val f = fansub?.trim()
    if (!f.isNullOrBlank()) return f
    val lang = language?.takeIf { it.isNotBlank() }?.uppercase()
    return if (lang != null) "Kaynak ${index + 1} • $lang" else "Kaynak ${index + 1}"
}

@Composable
private fun FansubSheet(
    episode: Episode,
    onSelect: (VideoSource) -> Unit,
    onDismiss: () -> Unit,
) {
    val sources = episode.playableSources()
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    BackHandler(onBack = onDismiss)

    Box(
        Modifier.fillMaxSize().background(Color(0xCC000000)),
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
            )
            Text(
                text = "Bölüm ${formatNumber(episode.episodeNumber)}",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
            )
            sources.forEachIndexed { idx, source ->
                val mod = if (idx == 0)
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).focusRequester(firstFocus)
                else
                    Modifier.fillMaxWidth().padding(vertical = 5.dp)
                androidx.tv.material3.Surface(
                    onClick = { onSelect(source) },
                    modifier = mod,
                    shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
                    colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                        containerColor = Color(0x22FFFFFF),
                        contentColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedContentColor = Color.Black,
                    ),
                ) {
                    Text(
                        text = source.fansubLabel(idx),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Backdrop(detail: AnimeDetail) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        val img = detail.backdrop ?: detail.poster
        if (!img.isNullOrBlank()) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(img)
                    .size(1280, 720)
                    .scale(coil.size.Scale.FILL)
                    .build(),
                contentDescription = detail.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to Color(0x99000000),
                        1f to Color.Black,
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xBB000000),
                        0.6f to Color.Transparent,
                    )
                )
        )
    }
}

@Composable
private fun DetailMeta(
    detail: AnimeDetail,
    inWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
    topFocus: FocusRequester,
    onTopFocused: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 48.dp)) {
        Text(
            text = detail.name,
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 40.sp,
        )
        if (!detail.nameEnglish.isNullOrBlank() && detail.nameEnglish != detail.name) {
            Text(
                text = detail.nameEnglish,
                color = Color(0xFFAAAAAA),
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            detail.year?.let { Pill("$it") }
            detail.rating?.let { Pill("★ ${"%.1f".format(it)}") }
            detail.runtime?.let { Pill("${it}dk") }
            if (detail.seriesEnded) Pill("Bitti") else if (detail.isSeries) Pill("Devam")
        }
        // Buton bilgilerin hemen altında (yukarıda) — backdrop kısaldığı için scroll=0'da
        // hem fotoğraf hem buton aynı anda görünür; bölümlerden YUKARI focus buraya gelince
        // ekran en üste kayar ve fotoğraf geri görünür.
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionPill(
                text = if (inWatchlist) "Listemden Çıkar" else "Listeme Ekle",
                onClick = onToggleWatchlist,
                modifier = Modifier
                    .focusRequester(topFocus)
                    .onFocusChanged { if (it.isFocused) onTopFocused() },
            )
        }
        if (detail.genres.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = detail.genres.take(6).joinToString(" • "),
                color = Color(0xFFCCCCCC),
                fontSize = 13.sp,
            )
        }
        if (!detail.description.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = detail.description,
                color = Color(0xFFE0E0E0),
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(860.dp),
            )
        }
    }
}

@Composable
private fun Pill(text: String) {
    Box(
        Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x22FFFFFF))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ActionPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = Color(0x33FFFFFF),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun SeasonSelector(seasons: List<SeasonInfo>, current: Int, onSelect: (SeasonInfo) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(seasons, key = { it.id }) { s ->
            val selected = s.number == current
            androidx.tv.material3.Surface(
                onClick = { onSelect(s) },
                shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
                colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
                    containerColor = if (selected) Color(0xFFE53935) else Color(0x22FFFFFF),
                    contentColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Color.Black,
                ),
            ) {
                Text(
                    text = s.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, onPlay: () -> Unit, focusUp: FocusRequester? = null) {
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (focused) Color(0xFF1C1C1E) else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (focused) Color.White.copy(alpha = 0.7f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            // İlk bölümde YUKARI focus'unu deterministik olarak üst butona yönlendir.
            // Key-event ile manuel requestFocus güvenilmezdi; focusProperties kesin çalışır.
            .focusProperties { if (focusUp != null) up = focusUp }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                // KeyUp'ta tetikle: aksi halde fansub menüsü açılınca, aynı basışın
                // KeyUp'ı yeni odaklanan menü öğesine düşüp player'ı erkenden açıyordu.
                if (ev.type == KeyEventType.KeyUp &&
                    (ev.key == Key.Enter || ev.key == Key.DirectionCenter || ev.key == Key.NumPadEnter)
                ) {
                    onPlay(); true
                } else false
            }
            .padding(10.dp),
    ) {
        Box(
            Modifier
                .width(140.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1E2A))
        ) {
            if (!episode.poster.isNullOrBlank()) {
                AsyncImage(
                    model = episode.poster,
                    contentDescription = episode.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Text(
                text = formatNumber(episode.episodeNumber),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC000000))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = episode.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!episode.description.isNullOrBlank()) {
                Text(
                    text = episode.description,
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatNumber(n: Float): String =
    if (n == n.toInt().toFloat()) n.toInt().toString() else n.toString()

@Composable
private fun CenterText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(32.dp),
        )
    }
}
