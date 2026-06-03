package com.alifzys.an1mecix.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alifzys.an1mecix.data.api.TauVideoService
import com.alifzys.an1mecix.data.repository.AnimeRepository
import com.alifzys.an1mecix.data.repository.UserDataRepository
import com.alifzys.an1mecix.domain.model.AnimeDetail
import com.alifzys.an1mecix.domain.model.Episode
import com.alifzys.an1mecix.domain.model.ResolvedStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Error(val message: String) : DetailUiState
    data class Ready(
        val detail: AnimeDetail,
        val inWatchlist: Boolean,
        val seasonLoading: Boolean = false,
    ) : DetailUiState
}

class DetailViewModel(
    private val titleId: Int,
    private val animeRepo: AnimeRepository,
    private val userRepo: UserDataRepository,
    private val tau: TauVideoService,
) : ViewModel() {

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        load(seasonNumber = 1)
    }

    fun load(seasonNumber: Int) {
        viewModelScope.launch {
            _state.value = DetailUiState.Loading
            try {
                val detail = animeRepo.detail(titleId, seasonNumber)
                _state.value = DetailUiState.Ready(detail, inWatchlist = false)
            } catch (e: Exception) {
                _state.value = DetailUiState.Error(e.message ?: "Hata")
            }
        }
    }

    /** Sezon değiştir — ekranı boşaltmadan, mevcut içeriği koruyarak. */
    fun selectSeason(seasonNumber: Int) {
        val cur = _state.value as? DetailUiState.Ready
        if (cur == null) { load(seasonNumber); return }
        if (cur.detail.currentSeason == seasonNumber) return
        viewModelScope.launch {
            _state.value = cur.copy(seasonLoading = true)
            try {
                val detail = animeRepo.detail(titleId, seasonNumber)
                _state.value = DetailUiState.Ready(detail, cur.inWatchlist, seasonLoading = false)
            } catch (_: Exception) {
                _state.value = cur.copy(seasonLoading = false)
            }
        }
    }

    fun toggleWatchlist() {
        val cur = _state.value as? DetailUiState.Ready ?: return
        viewModelScope.launch {
            userRepo.toggleWatchlist(cur.detail, cur.inWatchlist)
            _state.value = cur.copy(inWatchlist = !cur.inWatchlist)
        }
    }

    class Factory(
        private val titleId: Int,
        private val animeRepo: AnimeRepository,
        private val userRepo: UserDataRepository,
        private val tau: TauVideoService,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DetailViewModel(titleId, animeRepo, userRepo, tau) as T
    }
}
