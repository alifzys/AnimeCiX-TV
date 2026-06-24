package com.alifzys.an1mecix.ui.search

import android.app.Application
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.alifzys.an1mecix.AppContainer
import com.alifzys.an1mecix.data.repository.AnimeRepository
import com.alifzys.an1mecix.domain.model.AnimeCard
import com.alifzys.an1mecix.ui.components.PosterCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Alfabetik grid klavye (referans layout)
private val KB_ROWS = listOf(
    listOf("a","b","c","d","e","f"),
    listOf("g","h","i","j","k","l"),
    listOf("m","n","o","p","q","r"),
    listOf("s","t","u","v","w","x"),
    listOf("y","z","0","1","2","3"),
    listOf("4","5","6","7","8","9"),
)

// ---------- ViewModel ----------

class SearchViewModel(
    application: Application,
    private val repo: AnimeRepository,
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    private val _results = MutableStateFlow<List<AnimeCard>>(emptyList())
    val results: StateFlow<List<AnimeCard>> = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _popular = MutableStateFlow<List<AnimeCard>>(emptyList())
    val popular: StateFlow<List<AnimeCard>> = _popular.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private var job: Job? = null

    init {
        loadHistory()
        fetchPopular()
    }

    private fun loadHistory() {
        val json = prefs.getString("queries", "[]") ?: "[]"
        _recentSearches.value = try {
            Json.decodeFromString<List<String>>(json)
        } catch (_: Exception) { emptyList() }
    }

    private fun saveHistory() {
        prefs.edit().putString("queries", Json.encodeToString(_recentSearches.value)).apply()
    }

    fun removeFromHistory(query: String) {
        _recentSearches.value = _recentSearches.value.toMutableList().also { it.remove(query) }
        saveHistory()
    }

    private fun addToHistory(query: String) {
        if (query.isBlank() || query.length < 2) return
        val list = _recentSearches.value.toMutableList()
        list.remove(query)
        list.add(0, query)
        _recentSearches.value = list.take(10)
        saveHistory()
    }

    private fun fetchPopular() {
        viewModelScope.launch {
            try { _popular.value = repo.popular(24) } catch (_: Exception) {}
        }
    }

    /** Canlı arama — her tuş vuruşunda çağrılır, geçmişe YAZMAZ (önek kirliliği olmasın). */
    fun query(q: String) {
        job?.cancel()
        if (q.isBlank()) { _results.value = emptyList(); return }
        job = viewModelScope.launch {
            delay(350)
            _loading.value = true
            try {
                _results.value = repo.search(q)
            } catch (_: Exception) {
                _results.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    /** Anlamlı arama kaydı — kullanıcı bir sonucu açınca veya "ARA"ya basınca çağrılır. */
    fun recordSearch(q: String) = addToHistory(q.trim())

    class Factory(
        private val app: Application,
        private val repo: AnimeRepository,
    ) : ViewModelProvider.AndroidViewModelFactory(app) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            SearchViewModel(app, repo) as T
    }
}

// ---------- Screen ----------

@Composable
fun SearchScreen(
    container: AppContainer,
    onOpenDetail: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val vm: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(ctx.applicationContext as Application, container.animeRepo)
    )
    val results        by vm.results.collectAsStateWithLifecycle()
    val loading        by vm.loading.collectAsStateWithLifecycle()
    val popular        by vm.popular.collectAsStateWithLifecycle()
    val recentSearches by vm.recentSearches.collectAsStateWithLifecycle()

    var text by remember { mutableStateOf("") }
    val firstKeyFocus = remember { FocusRequester() }

    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { firstKeyFocus.requestFocus() }
    }

    fun handleKey(key: String) {
        text = when (key) {
            "SPACE" -> "$text "
            "⌫"    -> if (text.isNotEmpty()) text.dropLast(1) else text
            "ARA"   -> { vm.recordSearch(text); vm.query(text); text }
            else    -> text + key
        }
        if (key != "ARA") vm.query(text)
    }

    Row(Modifier.fillMaxSize().background(Color.Black)) {

        // ── Sol panel: klavye + son aramalar ──────────────────────────────────
        Column(
            Modifier
                .width(230.dp)
                .fillMaxHeight()
                .padding(start = 24.dp, top = 28.dp, end = 10.dp, bottom = 24.dp)
        ) {
            AlphaKeyboard(firstKeyFocus = firstKeyFocus, onKey = { handleKey(it) })

            if (recentSearches.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "SON ARAMALAR",
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                ) {
                    items(recentSearches, key = { it }) { query ->
                        RecentItem(
                            query = query,
                            onSelect = { text = query; vm.query(query) },
                            onDelete = { vm.removeFromHistory(query) },
                        )
                    }
                }
            }
        }

        // Dikey çizgi
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.07f))
        )

        // ── Sağ panel: arama girdi + popüler / sonuçlar ───────────────────────
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 28.dp, top = 28.dp, end = 48.dp, bottom = 24.dp)
        ) {
            // Arama başlığı
            if (text.isEmpty()) {
                Text(
                    text = "Aramak için yazın...",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Box(
                        Modifier
                            .padding(start = 3.dp)
                            .width(2.dp)
                            .height(22.dp)
                            .background(Color(0xFFE53935))
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                text.isEmpty() -> {
                    SectionLabel("POPÜLER ANİMELER")
                    Spacer(Modifier.height(10.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(152.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(popular, key = { it.id }) { card ->
                            PosterCard(item = card, onClick = { onOpenDetail(card.id) })
                        }
                    }
                }
                loading -> {
                    Text("Aranıyor...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
                }
                results.isEmpty() -> {
                    Text(
                        "\"$text\" için sonuç bulunamadı",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                    )
                }
                else -> {
                    SectionLabel("${results.size} SONUÇ")
                    Spacer(Modifier.height(10.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(152.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(results, key = { it.id }) { card ->
                            PosterCard(
                                item = card,
                                onClick = {
                                    // Sonuç açıldı → bu sorgu anlamlı, geçmişe tam haliyle kaydet
                                    vm.recordSearch(text)
                                    onOpenDetail(card.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------- Son Aramalar öğesi ----------

@Composable
private fun RecentItem(
    query: String,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            onClick = onSelect,
            modifier = Modifier.weight(1f).height(34.dp),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.White.copy(alpha = 0.06f),
                contentColor = Color.White.copy(alpha = 0.7f),
                focusedContainerColor = Color.White.copy(alpha = 0.14f),
                focusedContentColor = Color.White,
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text("◷ ", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                Text(
                    text = query,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(3.dp))
        Surface(
            onClick = onDelete,
            modifier = Modifier.width(28.dp).height(34.dp),
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(6.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = Color.White.copy(alpha = 0.3f),
                focusedContainerColor = Color(0xFF3A1A1A),
                focusedContentColor = Color(0xFFFF4444),
            ),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("×", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------- Klavye ----------

@Composable
private fun AlphaKeyboard(
    firstKeyFocus: FocusRequester,
    onKey: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.focusGroup(),
    ) {
        KB_ROWS.forEachIndexed { rowIdx, keys ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                keys.forEachIndexed { colIdx, key ->
                    val mod = if (rowIdx == 0 && colIdx == 0)
                        Modifier.weight(1f).height(34.dp).focusRequester(firstKeyFocus)
                    else
                        Modifier.weight(1f).height(34.dp)
                    KeyButton(key, mod, onKey = onKey)
                }
            }
        }
        // Alt satır: boşluk + sil
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            KeyButton("SPACE", Modifier.weight(3f).height(34.dp), onKey = onKey)
            KeyButton("⌫",    Modifier.weight(2f).height(34.dp), onKey = onKey)
        }
    }
}

@Composable
private fun KeyButton(
    key: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    onKey: (String) -> Unit,
) {
    val label = when (key) {
        "SPACE" -> "⎵"
        else    -> key.uppercase()
    }
    Surface(
        onClick = { onKey(key) },
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(0.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (highlight) Color(0xFFE53935) else Color(0xFF1C1C1E),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ---------- Yardımcılar ----------

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.35f),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
    )
}
