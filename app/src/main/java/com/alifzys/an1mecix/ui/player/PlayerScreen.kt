package com.alifzys.an1mecix.ui.player

import android.content.Context
import android.view.KeyEvent as AKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.alifzys.an1mecix.AppContainer
import com.alifzys.an1mecix.domain.model.Episode
import com.alifzys.an1mecix.domain.model.StreamQuality
import com.alifzys.an1mecix.domain.model.VideoSource
import com.alifzys.an1mecix.ui.components.FullScreenLoading
import kotlinx.coroutines.delay

private val SHOW_KEYS = setOf(
    AKeyEvent.KEYCODE_DPAD_LEFT, AKeyEvent.KEYCODE_DPAD_RIGHT,
    AKeyEvent.KEYCODE_DPAD_UP, AKeyEvent.KEYCODE_DPAD_DOWN,
    AKeyEvent.KEYCODE_DPAD_CENTER, AKeyEvent.KEYCODE_ENTER,
    AKeyEvent.KEYCODE_NUMPAD_ENTER, AKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
)

// Opening/Ending atlama pencereleri (saniye). API intro/outro markörü vermediği
// için sezgisel: opening bölüm başında, ending bölüm sonunda.
private const val OPENING_SHOW_START = 4L      // bu saniyeden sonra bandı göster
private const val OPENING_SHOW_END = 85L       // bu saniyeye kadar göster
private const val OPENING_SKIP_TO_MS = 90_000L // "Atla" → buraya sar
private const val ENDING_SHOW_MIN = 4L         // bitişe kalan sn alt sınır
private const val ENDING_SHOW_MAX = 90L        // bitişe kalan sn üst sınır
private const val SKIP_COUNTDOWN = 10          // geri sayım (sn)

@androidx.media3.common.util.UnstableApi
@Composable
fun PlayerScreen(
    container: AppContainer,
    titleId: Int,
    seasonNumber: Int,
    episodeId: Int,
    sourceId: Int,
    onBack: () -> Unit,
) {
    val vm: PlayerViewModel = viewModel(
        key = "player-$titleId-$seasonNumber-$episodeId-$sourceId",
        factory = PlayerViewModel.Factory(
            titleId, seasonNumber, episodeId, sourceId,
            container.animeRepo, container.userRepo, container.tauResolver
        )
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val s = state) {
            PlayerUiState.Loading -> FullScreenLoading()
            is PlayerUiState.Error -> CenterText("Hata: ${s.message}")
            is PlayerUiState.Ready -> PlayerContent(
                state = s,
                onSelectQuality = vm::selectQuality,
                onSelectSource = vm::selectSource,
                onPlayEpisode = vm::playEpisode,
                onSaveProgress = vm::saveProgress,
                onBack = onBack,
            )
        }
    }
}

@androidx.media3.common.util.UnstableApi
@Composable
private fun PlayerContent(
    state: PlayerUiState.Ready,
    onSelectQuality: (StreamQuality) -> Unit,
    onSelectSource: (VideoSource) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onSaveProgress: (Float, Float) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    // Önceki / sonraki bölüm
    val episodes = state.detail.episodes
    val epIdx = remember(state.episode.id) { episodes.indexOfFirst { it.id == state.episode.id } }
    val prevEp = if (epIdx > 0) episodes[epIdx - 1] else null
    val nextEp = if (epIdx < episodes.size - 1) episodes[epIdx + 1] else null

    val exoPlayer = remember {
        val ds = DefaultHttpDataSource.Factory().apply {
            setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
            )
            state.stream.referer?.let { setDefaultRequestProperties(mapOf("Referer" to it)) }
        }
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(ds))
            .build()
            .also { p ->
                // Görüntü iyileştirme (ayarlardan) — GL shader pipeline.
                val enhance = VideoEnhance.from(
                    context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                        .getString("video_enhance", "off")
                )
                android.util.Log.i("ACXFX", "enhance pref = $enhance")
                if (enhance != VideoEnhance.OFF) {
                    try {
                        p.setVideoEffects(listOf(VideoEnhanceEffect(enhance)))
                        android.util.Log.i("ACXFX", "setVideoEffects OK -> $enhance")
                    } catch (e: Throwable) {
                        android.util.Log.e("ACXFX", "setVideoEffects FAILED", e)
                    }
                }
            }
    }

    LaunchedEffect(state.currentQuality.url) {
        exoPlayer.setMediaItem(MediaItem.fromUri(state.currentQuality.url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        state.resumeAt?.let { exoPlayer.seekTo((it * 1000).toLong()) }
    }

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            positionMs = exoPlayer.currentPosition
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            isPlaying = exoPlayer.isPlaying
        }
    }

    LaunchedEffect(state.episode.id) {
        while (true) {
            delay(10_000)
            if (durationMs > 0) onSaveProgress(positionMs / 1000f, durationMs / 1000f)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (durationMs > 0) onSaveProgress(positionMs / 1000f, durationMs / 1000f)
            exoPlayer.release()
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var settingsOpen by remember { mutableStateOf(false) }
    var speedSheetOpen by remember { mutableStateOf(false) }
    var fansubSheetOpen by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1f) }
    val playFocus = remember { FocusRequester() }
    val barFocus = remember { FocusRequester() }
    val rootFocus = remember { FocusRequester() }

    // ── Opening / Ending atlama (ayarlardan) ───────────────────────────────
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val skipOpeningOn = remember { prefs.getBoolean("skip_opening", true) }
    val skipEndingOn = remember { prefs.getBoolean("skip_ending", true) }
    var openingHandled by remember(state.episode.id) { mutableStateOf(false) }
    var endingHandled by remember(state.episode.id) { mutableStateOf(false) }

    val posSec = positionMs / 1000
    val durSec = durationMs / 1000
    val openingActive = skipOpeningOn && !openingHandled && state.detail.isSeries &&
        posSec in OPENING_SHOW_START..OPENING_SHOW_END
    val endingActive = skipEndingOn && !endingHandled && nextEp != null &&
        durSec > 0L && (durSec - posSec) in ENDING_SHOW_MIN..ENDING_SHOW_MAX
    val skipActive = openingActive || endingActive
    val anySheetOpen = settingsOpen || speedSheetOpen || fansubSheetOpen

    LaunchedEffect(controlsVisible, anySheetOpen, skipActive) {
        if (controlsVisible && !anySheetOpen && !skipActive) {
            delay(4_000)
            controlsVisible = false
        }
    }

    // Kontroller kapandığında root focus'a dön
    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            delay(50)
            runCatching { rootFocus.requestFocus() }
        }
    }

    BackHandler {
        when {
            settingsOpen    -> { settingsOpen = false; controlsVisible = true }
            speedSheetOpen  -> { speedSheetOpen = false; controlsVisible = true }
            fansubSheetOpen -> { fansubSheetOpen = false; controlsVisible = true }
            openingActive   -> openingHandled = true
            endingActive    -> endingHandled = true
            controlsVisible -> controlsVisible = false
            else -> onBack()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .focusRequester(rootFocus)
            .focusable(enabled = !anySheetOpen && !skipActive)
            // Kontroller gizliyken herhangi bir tuşa basılınca göster.
            // Kontroller açıkken yön tuşlarını YUTMUYORUZ → TV focus traversal çalışsın.
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
                val kc = ev.nativeKeyEvent.keyCode
                if (!controlsVisible && kc in SHOW_KEYS) {
                    controlsVisible = true
                    return@onKeyEvent true  // bir defalık tüket
                }
                false
            }
    ) {
        // Video görüntüsü
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                }
            },
        )

        // Kontrol overlay'i
        AnimatedVisibility(
            visible = controlsVisible && !anySheetOpen && !skipActive,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
        ) {
            LaunchedEffect(Unit) { runCatching { barFocus.requestFocus() } }
            PlayerOverlay(
                title = state.detail.name,
                seasonNumber = state.episode.seasonNumber,
                episodeNumber = state.episode.episodeNumber,
                isPlaying = { isPlaying },
                positionMs = { positionMs },
                durationMs = { durationMs },
                qualityLabel = state.currentQuality.label,
                currentSpeed = currentSpeed,
                fansubLabel = state.currentSource.shortFansubLabel(),
                hasMultipleSources = state.sources.size > 1,
                prevEp = prevEp,
                nextEp = nextEp,
                playFocus = playFocus,
                barFocus = barFocus,
                onSeekBack = { exoPlayer.seekTo((positionMs - 10_000).coerceAtLeast(0)) },
                onSeekFwd = { exoPlayer.seekTo((positionMs + 10_000).coerceAtMost(durationMs)) },
                onTogglePlay = { exoPlayer.playWhenReady = !exoPlayer.playWhenReady },
                onOpenSettings = { settingsOpen = true },
                onOpenSpeed = { speedSheetOpen = true },
                onOpenFansub = { fansubSheetOpen = true },
                onPlayEpisode = onPlayEpisode,
            )
        }

        // Kalite seçici
        if (settingsOpen) {
            QualitySheet(
                qualities = state.stream.qualities,
                current = state.currentQuality,
                onSelect = { onSelectQuality(it); settingsOpen = false },
                onDismiss = { settingsOpen = false },
            )
        }

        // Hız seçici
        if (speedSheetOpen) {
            SpeedSheet(
                current = currentSpeed,
                onSelect = { speed ->
                    currentSpeed = speed
                    exoPlayer.setPlaybackParameters(
                        androidx.media3.common.PlaybackParameters(speed)
                    )
                    speedSheetOpen = false
                },
                onDismiss = { speedSheetOpen = false },
            )
        }

        // Fansub (kaynak) seçici
        if (fansubSheetOpen) {
            FansubSheet(
                sources = state.sources,
                current = state.currentSource,
                onSelect = { src -> onSelectSource(src); fansubSheetOpen = false },
                onDismiss = { fansubSheetOpen = false },
            )
        }

        // Opening / Ending atlama bandı (10 sn geri sayım)
        if (openingActive) {
            SkipPrompt(
                label = "Opening Atla",
                onSkipNow = {
                    exoPlayer.seekTo(OPENING_SKIP_TO_MS)
                    openingHandled = true
                },
                onDismiss = { openingHandled = true },
            )
        } else if (endingActive) {
            SkipPrompt(
                label = "Sonraki Bölüm",
                onSkipNow = { nextEp?.let { onPlayEpisode(it) }; endingHandled = true },
                onDismiss = { endingHandled = true },
            )
        }
    }
}

// ---------- Overlay ----------

@Composable
private fun PlayerOverlay(
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
    prevEp: Episode?,
    nextEp: Episode?,
    playFocus: FocusRequester,
    barFocus: FocusRequester,
    onSeekBack: () -> Unit,
    onSeekFwd: () -> Unit,
    onTogglePlay: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenFansub: () -> Unit,
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
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 40.dp, end = 40.dp, bottom = 20.dp),
        ) {
            ProgressBar(
                pct = pct.value,
                barFocus = barFocus,
                onSeekBack = onSeekBack,
                onSeekFwd = onSeekFwd,
                onTogglePlay = onTogglePlay,
            )

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
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
) {
    // Birincil oynatıcı odağı: kontroller açılınca buraya odaklanılır.
    // SOL/SAĞ → ileri-geri sarma, OK → duraklat/devam. AŞAĞI → buton satırı.
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
                    AKeyEvent.KEYCODE_NUMPAD_ENTER,
                    AKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { onTogglePlay(); true }
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

// ---------- Kalite seçici ----------

@Composable
private fun QualitySheet(
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

@Composable
private fun SpeedSheet(
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

/** Pill için kısa fansub etiketi. Temiz ve kısaysa adı, kredi metniyse "Fansub". */
private fun VideoSource.shortFansubLabel(): String {
    val f = fansub?.trim()
    if (!f.isNullOrBlank()) {
        // repository temizliyor; pill'e sığması için yine de kısalt
        return if (f.length <= 20) f else f.take(19).trimEnd() + "…"
    }
    return language?.takeIf { it.isNotBlank() }?.uppercase() ?: "Fansub"
}

/** Menü için tam fansub etiketi. */
private fun VideoSource.fullFansubLabel(index: Int): String {
    val f = fansub?.trim()
    if (!f.isNullOrBlank()) return f
    val lang = language?.takeIf { it.isNotBlank() }?.uppercase()
    return if (lang != null) "Kaynak ${index + 1} • $lang" else "Kaynak ${index + 1}"
}

@Composable
private fun FansubSheet(
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

// ---------- Opening / Ending atlama bandı ----------

@Composable
private fun SkipPrompt(
    label: String,
    onSkipNow: () -> Unit,
    onDismiss: () -> Unit,
) {
    val skipFocus = remember { FocusRequester() }
    var remaining by remember { mutableStateOf(SKIP_COUNTDOWN) }

    LaunchedEffect(Unit) {
        runCatching { skipFocus.requestFocus() }
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
        onSkipNow()  // geri sayım bitti → otomatik atla
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(end = 40.dp, bottom = 40.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0xE6101018), RoundedCornerShape(14.dp))
                .focusGroup()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            // "Atla" butonu — geri sayımlı
            Surface(
                onClick = onSkipNow,
                modifier = Modifier.focusRequester(skipFocus),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Color.Black,
                ),
            ) {
                Text(
                    text = "▶  $label  ($remaining)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            // İptal (✕)
            Surface(
                onClick = onDismiss,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color(0x33FFFFFF),
                    contentColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Color.Black,
                ),
            ) {
                Text(
                    text = "✕",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                )
            }
        }
    }
}

// ---------- Yardımcılar ----------

@Composable
private fun CenterText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = Color.White, fontSize = 16.sp)
    }
}

private fun fmtTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun numStr(n: Float): String =
    if (n == n.toInt().toFloat()) n.toInt().toString() else n.toString()
