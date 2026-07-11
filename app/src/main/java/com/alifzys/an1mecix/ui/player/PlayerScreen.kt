package com.alifzys.an1mecix.ui.player

import android.content.Context
import android.net.Uri
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.alifzys.an1mecix.AppContainer
import com.alifzys.an1mecix.domain.model.Episode
import com.alifzys.an1mecix.domain.model.StreamQuality
import com.alifzys.an1mecix.domain.model.Subtitle
import com.alifzys.an1mecix.domain.model.VideoSource
import com.alifzys.an1mecix.ui.components.FullScreenLoading
import kotlinx.coroutines.delay

// Opening atlama artık tau /most-sought markörlerinden gelir (gerçek intro from/to).
// Ending için markör yoksa "bitişe kalan sn" sezgisel geçişi devreye girer.
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
                // Görüntü iyileştirme + renk ayarları (ayarlardan) — GL efekt hattı.
                // Renk değerleri adım (-2..+2 / sıcaklık) olarak saklanır; burada efekte çevrilir.
                val settingsPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val enhance = VideoEnhance.from(settingsPrefs.getString("video_enhance", "off"))
                val anime4kScale = settingsPrefs.getInt("anime4k_scale", 150)
                val effects = buildList<Effect> {
                    if (enhance != VideoEnhance.OFF) add(VideoEnhanceEffect(enhance, anime4kScale))
                    // Renk Canlandırma (otomatik) — soluk eski anime renklerini canlandırır.
                    when (settingsPrefs.getInt("color_revive", 0)) {
                        1 -> add(ColorReviveEffect(0.5f))
                        2 -> add(ColorReviveEffect(1.0f))
                    }
                }
                if (effects.isNotEmpty()) {
                    try {
                        p.setVideoEffects(effects)
                        android.util.Log.i("ACXFX", "setVideoEffects OK -> ${effects.size} efekt")
                    } catch (e: Throwable) {
                        android.util.Log.e("ACXFX", "setVideoEffects FAILED", e)
                    }
                }
            }
    }

    // Hangi bölüm için hazırlık yapıldığını izle: aynı bölümde sadece KALİTE
    // değiştiğinde baştan başlamasın, mevcut konumdan devam etsin.
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }

    var preparedEpisodeId by remember { mutableStateOf(-1) }
    LaunchedEffect(state.currentQuality.url) {
        val sameEpisode = state.episode.id == preparedEpisodeId
        // setMediaItem'dan ÖNCE: currentPosition hâlâ eski (oynayan) videonun konumu.
        val keepPositionMs = if (sameEpisode) exoPlayer.currentPosition else 0L
        exoPlayer.setMediaItem(buildMediaItem(state.currentQuality.url, state.stream.subtitles))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        val seekTargetMs = if (sameEpisode) keepPositionMs
            else state.resumeAt?.let { (it * 1000).toLong() } ?: 0L
        if (seekTargetMs > 0) exoPlayer.seekTo(seekTargetMs)
        // Yeni bölüme geçerken eski bölümün konum/süresi UI'da KALMASIN. Aksi halde
        // ~500ms boyunca eski (sona yakın) değerlerle "bitişe kalan sn" sezgisi yeni
        // bölümün BAŞINDA "Sonraki Bölüm" bandını açıyordu. duration=0 → sezgi susar.
        positionMs = seekTargetMs
        durationMs = 0L
        isPlaying = true
        preparedEpisodeId = state.episode.id
    }

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
    var subtitleSheetOpen by remember { mutableStateOf(false) }
    var subtitleEditorOpen by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableFloatStateOf(1f) }
    val playFocus = remember { FocusRequester() }
    val barFocus = remember { FocusRequester() }
    val rootFocus = remember { FocusRequester() }

    // Altyazı: varsa Türkçe, yoksa ilk parça varsayılan (buildMediaItem ile aynı seçim).
    val defaultSubtitle = remember(state.stream.subtitles) {
        state.stream.subtitles.firstOrNull { it.language?.lowercase()?.startsWith("tr") == true }
            ?: state.stream.subtitles.firstOrNull()
    }
    var currentSubtitle by remember(state.stream.subtitles) { mutableStateOf(defaultSubtitle) }

    // OK/Center: oynat-duraklat. Duraklatınca kontrolleri göster, oynatınca gizle.
    fun togglePlayback() {
        val wasPlaying = exoPlayer.playWhenReady
        exoPlayer.playWhenReady = !wasPlaying
        controlsVisible = wasPlaying
    }

    // Altyazı seçimi — videoyu yeniden yüklemeden metin parçasını aç/kapat/değiştir.
    fun applySubtitle(sub: Subtitle?) {
        currentSubtitle = sub
        val p = exoPlayer.trackSelectionParameters.buildUpon()
        if (sub == null) {
            p.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            p.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            sub.language?.let { lang -> p.setPreferredTextLanguage(lang) }
        }
        exoPlayer.trackSelectionParameters = p.build()
    }

    // ── Scrubbing: hızlanan + smooth ileri-geri sarma ───────────────────────
    // Basılı tutarken HER tuşta seekTo yapmıyoruz (her seek buffer'lar → kasar).
    // Sadece önizleme hedefini kaydırıyoruz; tuş tekrarı arttıkça adım büyür
    // (hızlanır). Basış durunca (kısa süre yeni tuş gelmezse) TEK seferde hedefe
    // seek edilip normal hızda devam edilir.
    var scrubbing by remember { mutableStateOf(false) }
    var scrubTargetMs by remember { mutableLongStateOf(0L) }
    var scrubWasPlaying by remember { mutableStateOf(true) }

    fun scrubStep(dir: Int, repeatCount: Int) {
        if (!scrubbing) {
            scrubbing = true
            scrubWasPlaying = exoPlayer.playWhenReady
            scrubTargetMs = exoPlayer.currentPosition
            exoPlayer.playWhenReady = false
        }
        // Ne kadar uzun basılı → o kadar büyük adım (hızlanma).
        val step = when {
            repeatCount == 0 -> 10_000L
            repeatCount < 5 -> 15_000L
            repeatCount < 10 -> 30_000L
            repeatCount < 18 -> 60_000L
            else -> 120_000L
        }
        val dur = durationMs.takeIf { it > 0 } ?: exoPlayer.duration.coerceAtLeast(0L)
        val upper = if (dur > 0) dur else Long.MAX_VALUE
        scrubTargetMs = (scrubTargetMs + dir * step).coerceIn(0L, upper)
    }

    // Sarma durunca hedefe seek + önceki oynatma durumuna dön.
    LaunchedEffect(scrubbing, scrubTargetMs) {
        if (scrubbing) {
            delay(300)
            exoPlayer.seekTo(scrubTargetMs)
            positionMs = scrubTargetMs
            exoPlayer.playWhenReady = scrubWasPlaying
            isPlaying = scrubWasPlaying
            scrubbing = false
        }
    }

    // ── Opening / Ending atlama (ayarlardan) ───────────────────────────────
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    // Altyazı stili — canlı düzenlenebilir; AndroidView update'inde subtitleView'a uygulanır.
    var subSizeFrac by remember { mutableFloatStateOf(prefs.getFloat(SubtitleStyle.KEY_SIZE, SubtitleStyle.DEF_SIZE)) }
    var subBottomFrac by remember { mutableFloatStateOf(prefs.getFloat(SubtitleStyle.KEY_BOTTOM, SubtitleStyle.DEF_BOTTOM)) }
    var subFontId by remember { mutableStateOf(prefs.getString(SubtitleStyle.KEY_FONT, SubtitleStyle.DEF_FONT) ?: SubtitleStyle.DEF_FONT) }
    var subFill by remember { mutableIntStateOf(prefs.getInt(SubtitleStyle.KEY_FILL, SubtitleStyle.DEF_FILL)) }
    var subEdgeColor by remember { mutableIntStateOf(prefs.getInt(SubtitleStyle.KEY_EDGE_COLOR, SubtitleStyle.DEF_EDGE_COLOR)) }
    var subEdgeType by remember { mutableIntStateOf(prefs.getInt(SubtitleStyle.KEY_EDGE_TYPE, SubtitleStyle.DEF_EDGE_TYPE)) }
    val subTypeface = remember(subFontId) { SubtitleStyle.typefaceFor(context, subFontId) }
    val availableFonts = remember { SubtitleStyle.availableFonts(context) }

    val skipOpeningOn = remember { prefs.getBoolean("skip_opening", true) }
    val skipEndingOn = remember { prefs.getBoolean("skip_ending", true) }
    var openingHandled by remember(state.episode.id) { mutableStateOf(false) }
    var endingHandled by remember(state.episode.id) { mutableStateOf(false) }

    val posSec = positionMs / 1000
    val durSec = durationMs / 1000
    // Gerçek intro/outro zamanları tau /most-sought'tan (varsa). Yoksa opening bandı gösterilmez.
    val markers = state.stream.markers
    val introFrom = markers?.introFrom
    val introTo = markers?.introTo
    val outroFrom = markers?.outroFrom
    val outroTo = markers?.outroTo

    val openingActive = skipOpeningOn && !openingHandled && state.detail.isSeries &&
        introFrom != null && introTo != null && posSec in introFrom..introTo
    // Ending: markör varsa ona göre; yoksa "bitişe kalan sn" sezgisi.
    val endingByMarker = outroFrom != null &&
        posSec >= outroFrom && (outroTo == null || posSec <= outroTo)
    val endingByHeuristic = outroFrom == null &&
        durSec > 0L && posSec > ENDING_SHOW_MAX &&
        (durSec - posSec) in ENDING_SHOW_MIN..ENDING_SHOW_MAX
    val endingActive = skipEndingOn && !endingHandled && nextEp != null &&
        (endingByMarker || endingByHeuristic)
    val skipActive = openingActive || endingActive
    val anySheetOpen = settingsOpen || speedSheetOpen || fansubSheetOpen ||
        subtitleSheetOpen || subtitleEditorOpen

    // Otomatik gizleme yalnızca OYNARKEN; duraklatıldığında kontroller kalsın.
    LaunchedEffect(controlsVisible, anySheetOpen, skipActive, isPlaying) {
        if (controlsVisible && isPlaying && !anySheetOpen && !skipActive) {
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
            settingsOpen     -> { settingsOpen = false; controlsVisible = true }
            speedSheetOpen   -> { speedSheetOpen = false; controlsVisible = true }
            fansubSheetOpen  -> { fansubSheetOpen = false; controlsVisible = true }
            subtitleSheetOpen -> { subtitleSheetOpen = false; controlsVisible = true }
            subtitleEditorOpen -> { subtitleEditorOpen = false; controlsVisible = true }
            openingActive    -> openingHandled = true
            endingActive     -> endingHandled = true
            controlsVisible  -> controlsVisible = false
            else -> onBack()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .focusRequester(rootFocus)
            .focusable(enabled = !anySheetOpen && !skipActive)
            // Kontroller gizliyken tuş davranışı:
            //  OK/Center → oynat-duraklat (+ duraklayınca kontroller gelir)
            //  YUKARI    → sadece kontrolleri göster (duraklatmadan)
            //  SOL/SAĞ   → doğrudan ±10 sn sar (+ kontroller gelir)
            //  AŞAĞI     → hiçbir şey (kullanıcı: aşağı = gizle)
            // Kontroller açıkken yön tuşlarını YUTMUYORUZ → TV focus traversal çalışsın.
            .onKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onKeyEvent false
                val kc = ev.nativeKeyEvent.keyCode
                if (!controlsVisible) {
                    when (kc) {
                        AKeyEvent.KEYCODE_DPAD_LEFT -> {
                            controlsVisible = true
                            scrubStep(-1, ev.nativeKeyEvent.repeatCount)
                            return@onKeyEvent true
                        }
                        AKeyEvent.KEYCODE_DPAD_RIGHT -> {
                            controlsVisible = true
                            scrubStep(1, ev.nativeKeyEvent.repeatCount)
                            return@onKeyEvent true
                        }
                        AKeyEvent.KEYCODE_DPAD_CENTER,
                        AKeyEvent.KEYCODE_ENTER,
                        AKeyEvent.KEYCODE_NUMPAD_ENTER,
                        AKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            togglePlayback()
                            return@onKeyEvent true
                        }
                        AKeyEvent.KEYCODE_DPAD_UP -> {
                            controlsVisible = true
                            return@onKeyEvent true
                        }
                    }
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
            // Altyazı stili düzenleyiciden CANLI gelir; her state değişiminde uygulanır.
            update = { pv ->
                pv.subtitleView?.apply {
                    setFractionalTextSize(subSizeFrac)
                    // Kontroller açıkken altyazı ilerleme çubuğunun ÜSTÜne kalksın.
                    val lift = if (controlsVisible && !anySheetOpen && !skipActive)
                        SubtitleStyle.CONTROLS_LIFT else 0f
                    setBottomPaddingFraction((subBottomFrac + lift).coerceIn(0f, 0.9f))
                    setStyle(
                        CaptionStyleCompat(
                            subFill,
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                            subEdgeType,
                            subEdgeColor,
                            subTypeface,
                        )
                    )
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
                positionMs = { if (scrubbing) scrubTargetMs else positionMs },
                durationMs = { durationMs },
                qualityLabel = state.currentQuality.label,
                currentSpeed = currentSpeed,
                fansubLabel = state.currentSource.shortFansubLabel(),
                hasMultipleSources = state.sources.size > 1,
                hasSubtitles = state.stream.subtitles.isNotEmpty(),
                subtitleLabel = currentSubtitle?.let { it.language?.uppercase() ?: "Açık" } ?: "Kapalı",
                prevEp = prevEp,
                nextEp = nextEp,
                playFocus = playFocus,
                barFocus = barFocus,
                onScrub = { dir, repeatCount -> scrubStep(dir, repeatCount) },
                onTogglePlay = { togglePlayback() },
                onHideControls = { controlsVisible = false },
                onOpenSettings = { settingsOpen = true },
                onOpenSpeed = { speedSheetOpen = true },
                onOpenFansub = { fansubSheetOpen = true },
                onOpenSubtitle = { subtitleSheetOpen = true },
                onOpenSubtitleEditor = { subtitleEditorOpen = true },
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

        // Altyazı seçici
        if (subtitleSheetOpen) {
            SubtitleSheet(
                subtitles = state.stream.subtitles,
                current = currentSubtitle,
                onSelect = { sub -> applySubtitle(sub); subtitleSheetOpen = false },
                onDismiss = { subtitleSheetOpen = false },
            )
        }

        // Altyazı düzenleyici (boyut/konum/font/renk/gölge — canlı)
        if (subtitleEditorOpen) {
            SubtitleEditorSheet(
                sizeFrac = subSizeFrac,
                onSizeDelta = { d ->
                    subSizeFrac = (subSizeFrac + d * SubtitleStyle.STEP_SIZE)
                        .coerceIn(SubtitleStyle.MIN_SIZE, SubtitleStyle.MAX_SIZE)
                    prefs.edit().putFloat(SubtitleStyle.KEY_SIZE, subSizeFrac).apply()
                },
                bottomFrac = subBottomFrac,
                onBottomDelta = { d ->
                    subBottomFrac = (subBottomFrac + d * SubtitleStyle.STEP_BOTTOM)
                        .coerceIn(SubtitleStyle.MIN_BOTTOM, SubtitleStyle.MAX_BOTTOM)
                    prefs.edit().putFloat(SubtitleStyle.KEY_BOTTOM, subBottomFrac).apply()
                },
                fontId = subFontId,
                fonts = availableFonts,
                onFont = { subFontId = it; prefs.edit().putString(SubtitleStyle.KEY_FONT, it).apply() },
                fillColor = subFill,
                onFill = { subFill = it; prefs.edit().putInt(SubtitleStyle.KEY_FILL, it).apply() },
                edgeColor = subEdgeColor,
                onEdge = { subEdgeColor = it; prefs.edit().putInt(SubtitleStyle.KEY_EDGE_COLOR, it).apply() },
                edgeType = subEdgeType,
                onEdgeType = { subEdgeType = it; prefs.edit().putInt(SubtitleStyle.KEY_EDGE_TYPE, it).apply() },
                onReset = {
                    subSizeFrac = SubtitleStyle.DEF_SIZE
                    subBottomFrac = SubtitleStyle.DEF_BOTTOM
                    subFontId = SubtitleStyle.DEF_FONT
                    subFill = SubtitleStyle.DEF_FILL
                    subEdgeColor = SubtitleStyle.DEF_EDGE_COLOR
                    subEdgeType = SubtitleStyle.DEF_EDGE_TYPE
                    prefs.edit()
                        .putFloat(SubtitleStyle.KEY_SIZE, SubtitleStyle.DEF_SIZE)
                        .putFloat(SubtitleStyle.KEY_BOTTOM, SubtitleStyle.DEF_BOTTOM)
                        .putString(SubtitleStyle.KEY_FONT, SubtitleStyle.DEF_FONT)
                        .putInt(SubtitleStyle.KEY_FILL, SubtitleStyle.DEF_FILL)
                        .putInt(SubtitleStyle.KEY_EDGE_COLOR, SubtitleStyle.DEF_EDGE_COLOR)
                        .putInt(SubtitleStyle.KEY_EDGE_TYPE, SubtitleStyle.DEF_EDGE_TYPE)
                        .apply()
                },
                onDismiss = { subtitleEditorOpen = false; controlsVisible = true },
            )
        }

        // Opening / Ending atlama bandı (10 sn geri sayım)
        if (openingActive) {
            SkipPrompt(
                label = "Opening Atla",
                onSkipNow = {
                    introTo?.let { exoPlayer.seekTo(it * 1000) }
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

/**
 * Video + soft-sub altyazıları tek MediaItem'a bağlar. Altyazılar WebVTT (tau /vtt/);
 * varsa Türkçe olan, yoksa ilk parça varsayılan (DEFAULT) seçilir → PlayerView otomatik gösterir.
 */
private fun buildMediaItem(url: String, subtitles: List<Subtitle>): MediaItem {
    val builder = MediaItem.Builder().setUri(url)
    if (subtitles.isNotEmpty()) {
        val defaultIdx = subtitles
            .indexOfFirst { it.language?.lowercase()?.startsWith("tr") == true }
            .let { if (it >= 0) it else 0 }
        val configs = subtitles.mapIndexed { i, sub ->
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                .setMimeType(MimeTypes.TEXT_VTT)
                .setLanguage(sub.language)
                .setLabel(sub.label)
                .setSelectionFlags(if (i == defaultIdx) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }
        builder.setSubtitleConfigurations(configs)
    }
    return builder.build()
}

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
