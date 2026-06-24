package com.alifzys.an1mecix.ui.player

import android.content.Context
import android.view.KeyEvent as AKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
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

@androidx.media3.common.util.UnstableApi
@Composable
fun PlayerScreen(
    container: AppContainer,
    titleId: Int,
    seasonNumber: Int,
    episodeId: Int,
    sourceId: Int,
    onBack: () -> Unit,
    offline: Boolean = false,
) {
    val vm: PlayerViewModel = viewModel(
        key = "player-${if (offline) "off-" else ""}$titleId-$seasonNumber-$episodeId-$sourceId",
        factory = PlayerViewModel.Factory(
            titleId, seasonNumber, episodeId, sourceId,
            container.animeRepo, container.userRepo, container.tauResolver,
            container.downloadManager, offline,
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

    val isOffline = state.stream.provider == "offline"
    val exoPlayer = remember {
        val sourceFactory = if (isOffline) {
            // Lokal dosya (file://) — default data source file şemasını açar.
            DefaultMediaSourceFactory(context)
        } else {
            val ds = DefaultHttpDataSource.Factory().apply {
                setUserAgent(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
                )
                state.stream.referer?.let { setDefaultRequestProperties(mapOf("Referer" to it)) }
            }
            DefaultMediaSourceFactory(context).setDataSourceFactory(ds)
        }
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(sourceFactory)
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

    // Hangi bölüm için hazırlık yapıldığını izle: aynı bölümde sadece KALİTE
    // değiştiğinde baştan başlamasın, mevcut konumdan devam etsin.
    var preparedEpisodeId by remember { mutableStateOf(-1) }
    LaunchedEffect(state.currentQuality.url) {
        val sameEpisode = state.episode.id == preparedEpisodeId
        // setMediaItem'dan ÖNCE: currentPosition hâlâ eski (oynayan) videonun konumu.
        val keepPositionMs = if (sameEpisode) exoPlayer.currentPosition else 0L
        exoPlayer.setMediaItem(MediaItem.fromUri(state.currentQuality.url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        val seekTargetMs = if (sameEpisode) keepPositionMs
            else state.resumeAt?.let { (it * 1000).toLong() } ?: 0L
        if (seekTargetMs > 0) exoPlayer.seekTo(seekTargetMs)
        preparedEpisodeId = state.episode.id
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

// ---------- Yardımcılar ----------

/** Pill için kısa fansub etiketi. Temiz ve kısaysa adı, kredi metniyse "Fansub". */
private fun VideoSource.shortFansubLabel(): String {
    val f = fansub?.trim()
    if (!f.isNullOrBlank()) {
        // repository temizliyor; pill'e sığması için yine de kısalt
        return if (f.length <= 20) f else f.take(19).trimEnd() + "…"
    }
    return language?.takeIf { it.isNotBlank() }?.uppercase() ?: "Fansub"
}

@Composable
private fun CenterText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = Color.White, fontSize = 16.sp)
    }
}
