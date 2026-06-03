package com.alifzys.an1mecix.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.alifzys.an1mecix.domain.model.AnimeCard
import com.alifzys.an1mecix.domain.model.FeaturedItem
import com.alifzys.an1mecix.ui.components.FullScreenLoading
import com.alifzys.an1mecix.ui.components.PosterRow

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenDetail: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(container.animeRepo, container.userRepo)
    )
    val state by vm.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val s = state) {
            HomeUiState.Loading -> FullScreenLoading()
            is HomeUiState.Error -> CenterText("Hata: ${s.message}")
            is HomeUiState.Ready -> HomeContent(
                featured = s.data.featured.firstOrNull(),
                rows = s.data.rows,
                continueCards = s.continueWatching.map { it.toCard() }.distinctBy { it.id },
                onOpenDetail = onOpenDetail,
                onOpenSearch = onOpenSearch,
                onOpenCategories = onOpenCategories,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun HomeContent(
    featured: FeaturedItem?,
    rows: List<com.alifzys.an1mecix.domain.model.HomeRow>,
    continueCards: List<AnimeCard>,
    onOpenDetail: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().focusGroup(),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item {
            TopBar(onOpenSearch = onOpenSearch, onOpenCategories = onOpenCategories, onOpenSettings = onOpenSettings)
        }
        if (featured != null) {
            item { HeroBanner(featured = featured, onPlay = { onOpenDetail(featured.card.id) }) }
        }
        if (continueCards.isNotEmpty()) {
            item {
                Spacer(Modifier.height(32.dp))
                PosterRow(
                    title = "İzlemeye Devam Et",
                    items = continueCards,
                    onOpen = { onOpenDetail(it.id) },
                )
            }
        }
        items(rows, key = { it.id }) { row ->
            PosterRow(
                title = row.name,
                items = row.items,
                onOpen = { onOpenDetail(it.id) },
            )
        }
    }
}

@Composable
private fun TopBar(onOpenSearch: () -> Unit, onOpenCategories: () -> Unit, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 56.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "AnimeCiX",
            color = Color(0xFFE53935),
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.weight(1f))
        NavChip("KATEGORİLER", onOpenCategories)
        Spacer(Modifier.width(4.dp))
        NavChip("ARA", onOpenSearch)
        Spacer(Modifier.width(4.dp))
        NavChip("AYARLAR", onOpenSettings)
    }
}

@Composable
private fun NavChip(label: String, onClick: () -> Unit) {
    androidx.tv.material3.Surface(
        onClick = onClick,
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.5f),
            focusedContainerColor = Color.White.copy(alpha = 0.12f),
            focusedContentColor = Color.White,
        ),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun HeroBanner(featured: FeaturedItem, onPlay: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        if (!featured.card.backdrop.isNullOrBlank()) {
            AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(featured.card.backdrop)
                    .size(1280, 720)
                    .scale(coil.size.Scale.FILL)
                    .build(),
                contentDescription = featured.card.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        // Sol gradient: içerik alanını maskeler
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xFF000000),
                        0.55f to Color(0xCC000000),
                        0.78f to Color.Transparent,
                    )
                )
        )
        // Alt gradient: bir sonraki satıra geçiş
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black,
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 56.dp, bottom = 52.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            if (featured.genres.isNotEmpty()) {
                Text(
                    text = featured.genres.take(3).joinToString("  ·  ").uppercase(),
                    color = Color(0xFFE53935),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(10.dp))
            }
            Text(
                text = featured.card.name,
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 52.sp,
                modifier = Modifier.width(580.dp),
            )
            if (!featured.description.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = featured.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(520.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
            HeroButton(onClick = onPlay)
        }
    }
}

@Composable
private fun HeroButton(onClick: () -> Unit) {
    androidx.tv.material3.Surface(
        onClick = onClick,
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = Color.White,
            contentColor = Color.Black,
            focusedContainerColor = Color(0xFFDDDDDD),
            focusedContentColor = Color.Black,
        ),
    ) {
        Text(
            text = "▶  İncele",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 13.dp),
        )
    }
}

@Composable
private fun CenterText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}

private fun com.alifzys.an1mecix.data.local.entities.HistoryEntry.toCard(): AnimeCard = AnimeCard(
    id = titleId,
    name = titleName,
    poster = titlePoster,
    backdrop = titleBackdrop,
)
