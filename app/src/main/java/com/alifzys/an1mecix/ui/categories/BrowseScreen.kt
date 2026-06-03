package com.alifzys.an1mecix.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.alifzys.an1mecix.AppContainer
import com.alifzys.an1mecix.data.repository.AnimeRepository
import com.alifzys.an1mecix.domain.model.AnimeCard
import com.alifzys.an1mecix.ui.components.PosterCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BrowseViewModel(
    private val slug: String,
    private val repo: AnimeRepository,
) : ViewModel() {
    private val _items = MutableStateFlow<List<AnimeCard>>(emptyList())
    val items: StateFlow<List<AnimeCard>> = _items.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private var page = 1
    private var hasMore = true

    init { loadMore() }

    fun loadMore() {
        if (_loading.value || !hasMore) return
        viewModelScope.launch {
            _loading.value = true
            try {
                val (new, more) = repo.browseGenre(slug, page)
                // Sayfa geçişlerinde duplicate olabilir, LazyGrid patlamasın
                _items.value = (_items.value + new).distinctBy { it.id }
                hasMore = more
                if (more) page++
            } finally {
                _loading.value = false
            }
        }
    }

    class Factory(private val slug: String, private val repo: AnimeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BrowseViewModel(slug, repo) as T
    }
}

@Composable
fun BrowseScreen(
    container: AppContainer,
    slug: String,
    name: String,
    onOpenDetail: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val vm: BrowseViewModel = viewModel(
        key = "browse-$slug",
        factory = BrowseViewModel.Factory(slug, container.animeRepo),
    )
    val items by vm.items.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(48.dp)
    ) {
        Text(text = name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(180.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.id }) { card ->
                PosterCard(item = card, onClick = { onOpenDetail(card.id) })
            }
        }
    }
}
