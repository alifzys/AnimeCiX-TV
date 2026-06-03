package com.alifzys.an1mecix.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alifzys.an1mecix.data.local.entities.HistoryEntry
import com.alifzys.an1mecix.data.repository.AnimeRepository
import com.alifzys.an1mecix.data.repository.UserDataRepository
import com.alifzys.an1mecix.domain.model.HomeData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Ready(
        val data: HomeData,
        val continueWatching: List<HistoryEntry>,
    ) : HomeUiState
}

class HomeViewModel(
    private val animeRepo: AnimeRepository,
    private val userRepo: UserDataRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var loaded: HomeData? = null
    private var continueList: List<HistoryEntry> = emptyList()

    init {
        load()
        userRepo.continueWatching()
            .onEach { list ->
                continueList = list
                emit()
            }
            .launchIn(viewModelScope)
    }

    fun load() {
        viewModelScope.launch {
            _state.value = HomeUiState.Loading
            try {
                loaded = animeRepo.home()
                emit()
            } catch (e: Exception) {
                _state.value = HomeUiState.Error(e.message ?: "Bilinmeyen hata")
            }
        }
    }

    private fun emit() {
        val d = loaded ?: return
        _state.value = HomeUiState.Ready(d, continueList)
    }

    class Factory(
        private val animeRepo: AnimeRepository,
        private val userRepo: UserDataRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(animeRepo, userRepo) as T
    }
}
