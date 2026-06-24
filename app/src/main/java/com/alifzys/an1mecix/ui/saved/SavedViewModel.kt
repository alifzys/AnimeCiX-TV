package com.alifzys.an1mecix.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alifzys.an1mecix.data.download.DownloadManager
import com.alifzys.an1mecix.data.local.entities.SavedEpisodeEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SavedViewModel(
    private val downloads: DownloadManager,
) : ViewModel() {

    val items: StateFlow<List<SavedEpisodeEntry>> =
        downloads.saved().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun remove(episodeId: Int) = downloads.remove(episodeId)
    fun retry(episodeId: Int) = downloads.retry(episodeId)

    class Factory(
        private val downloads: DownloadManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SavedViewModel(downloads) as T
    }
}
