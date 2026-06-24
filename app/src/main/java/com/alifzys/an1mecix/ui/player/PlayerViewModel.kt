package com.alifzys.an1mecix.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alifzys.an1mecix.core.Constants
import com.alifzys.an1mecix.data.api.TauVideoService
import com.alifzys.an1mecix.data.download.DownloadManager
import com.alifzys.an1mecix.data.local.entities.HistoryEntry
import com.alifzys.an1mecix.data.repository.AnimeRepository
import com.alifzys.an1mecix.data.repository.UserDataRepository
import com.alifzys.an1mecix.domain.model.AnimeDetail
import com.alifzys.an1mecix.domain.model.Comment
import com.alifzys.an1mecix.domain.model.Episode
import com.alifzys.an1mecix.domain.model.ResolvedStream
import com.alifzys.an1mecix.domain.model.StreamQuality
import com.alifzys.an1mecix.domain.model.VideoSource
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Error(val message: String) : PlayerUiState
    data class Ready(
        val detail: AnimeDetail,
        val episode: Episode,
        val stream: ResolvedStream,
        val resumeAt: Float?,
        val currentQuality: StreamQuality,
        val comments: List<Comment>,
        val sources: List<VideoSource>,
        val currentSource: VideoSource,
    ) : PlayerUiState
}

class PlayerViewModel(
    private val titleId: Int,
    private val seasonNumber: Int,
    private val episodeId: Int,
    private val sourceId: Int,
    private val animeRepo: AnimeRepository,
    private val userRepo: UserDataRepository,
    private val tau: TauVideoService,
    private val downloads: DownloadManager? = null,
    private val offline: Boolean = false,
) : ViewModel() {

    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    init {
        if (offline) loadOffline() else load()
    }

    /** İndirilmiş bölümü lokal dosyadan oynat — ağ gerektirmez. */
    private fun loadOffline() {
        viewModelScope.launch {
            _state.value = PlayerUiState.Loading
            try {
                val entry = downloads?.get(episodeId)
                    ?: throw IllegalStateException("Kayıt bulunamadı")
                val path = entry.filePath?.takeIf { File(it).exists() }
                    ?: throw IllegalStateException("İndirilen dosya bulunamadı")
                val episode = Episode(
                    id = entry.episodeId,
                    seasonNumber = entry.seasonNumber,
                    episodeNumber = entry.episodeNumber,
                    name = entry.episodeName ?: "Bölüm",
                    poster = entry.episodePoster,
                    sources = emptyList(),
                )
                val detail = AnimeDetail(
                    id = entry.titleId,
                    name = entry.titleName,
                    nameEnglish = null,
                    description = null,
                    poster = entry.titlePoster,
                    backdrop = entry.titleBackdrop,
                    year = null,
                    rating = null,
                    runtime = null,
                    genres = emptyList(),
                    isSeries = true,
                    seriesEnded = false,
                    nextEpisodeDate = null,
                    nextEpisodeNumber = null,
                    trailerUrl = null,
                    seasons = emptyList(),
                    currentSeason = entry.seasonNumber,
                    episodes = listOf(episode),
                    cast = emptyList(),
                    related = emptyList(),
                )
                val quality = StreamQuality(
                    label = entry.quality ?: "İndirilen",
                    url = android.net.Uri.fromFile(File(path)).toString(),
                    size = entry.fileSize,
                )
                val stream = ResolvedStream(
                    provider = "offline",
                    qualities = listOf(quality),
                    referer = null,
                )
                val source = VideoSource(
                    id = entry.sourceId,
                    name = entry.titleName,
                    url = entry.sourceUrl,
                    type = "offline",
                )
                val resume = userRepo.progressFor(episode.id)
                    ?.takeIf { it.progressSec > 30f && it.durationSec > 0 && it.progressSec / it.durationSec < 0.95f }
                    ?.progressSec
                _state.value = PlayerUiState.Ready(
                    detail, episode, stream, resume, quality, emptyList(), listOf(source), source
                )
            } catch (e: Exception) {
                _state.value = PlayerUiState.Error(e.message ?: "Çevrimdışı oynatma hatası")
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = PlayerUiState.Loading
            try {
                // Doğru sezonun detayını çek → episodeId o sezonda olacak
                val detail = animeRepo.detail(titleId, seasonNumber)
                val episode = detail.episodes.firstOrNull { it.id == episodeId }
                    ?: detail.episodes.firstOrNull()
                    ?: throw IllegalStateException("Bölüm bulunamadı")
                val playable = episode.sources.filter { Constants.TAU_HOST in it.url }
                if (playable.isEmpty()) throw IllegalStateException("Oynatılabilir kaynak yok")
                val source = playable.firstOrNull { it.id == sourceId } ?: playable.first()
                val stream = tau.resolve(source.url)
                val resume = userRepo.progressFor(episode.id)
                    ?.takeIf { it.progressSec > 30f && it.durationSec > 0 && it.progressSec / it.durationSec < 0.95f }
                    ?.progressSec
                val comments = runCatching {
                    animeRepo.comments(detail.id, episode.seasonNumber, episode.episodeNumber.toInt())
                }.getOrDefault(emptyList())
                val q = stream.qualities.firstOrNull()
                    ?: throw IllegalStateException("Kalite seçeneği yok")
                _state.value = PlayerUiState.Ready(detail, episode, stream, resume, q, comments, playable, source)
            } catch (e: Exception) {
                _state.value = PlayerUiState.Error(e.message ?: "Hata")
            }
        }
    }

    fun selectQuality(q: StreamQuality) {
        val cur = _state.value as? PlayerUiState.Ready ?: return
        _state.value = cur.copy(currentQuality = q)
    }

    /** Fansub (kaynak) değiştir — aynı bölüm, farklı tau-video kaynağı. */
    fun selectSource(source: VideoSource) {
        val cur = _state.value as? PlayerUiState.Ready ?: return
        if (source.id == cur.currentSource.id) return
        viewModelScope.launch {
            try {
                val stream = tau.resolve(source.url)
                val q = stream.qualities.firstOrNull() ?: return@launch
                _state.value = cur.copy(stream = stream, currentSource = source, currentQuality = q)
            } catch (_: Exception) { /* mevcut kaynakta kal */ }
        }
    }

    fun playEpisode(ep: Episode) {
        viewModelScope.launch {
            val playable = ep.sources.filter { Constants.TAU_HOST in it.url }
            val source = playable.firstOrNull() ?: return@launch
            val stream = tau.resolve(source.url)
            val q = stream.qualities.firstOrNull() ?: return@launch
            val cur = _state.value as? PlayerUiState.Ready ?: return@launch
            val resume = userRepo.progressFor(ep.id)
                ?.takeIf { it.progressSec > 30f && it.durationSec > 0 && it.progressSec / it.durationSec < 0.95f }
                ?.progressSec
            val comments = runCatching {
                animeRepo.comments(cur.detail.id, ep.seasonNumber, ep.episodeNumber.toInt())
            }.getOrDefault(emptyList())
            _state.value = cur.copy(
                episode = ep, stream = stream, currentQuality = q,
                resumeAt = resume, comments = comments,
                sources = playable, currentSource = source,
            )
        }
    }

    fun saveProgress(progressSec: Float, durationSec: Float) {
        val cur = _state.value as? PlayerUiState.Ready ?: return
        viewModelScope.launch {
            userRepo.saveProgress(cur.detail, cur.episode, progressSec, durationSec)
        }
    }

    class Factory(
        private val titleId: Int,
        private val seasonNumber: Int,
        private val episodeId: Int,
        private val sourceId: Int,
        private val animeRepo: AnimeRepository,
        private val userRepo: UserDataRepository,
        private val tau: TauVideoService,
        private val downloads: DownloadManager? = null,
        private val offline: Boolean = false,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlayerViewModel(
                titleId, seasonNumber, episodeId, sourceId,
                animeRepo, userRepo, tau, downloads, offline,
            ) as T
    }
}
