package com.alifzys.an1mecix.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.alifzys.an1mecix.AppContainer
import com.alifzys.an1mecix.domain.model.GENRES
import com.alifzys.an1mecix.domain.model.GenreInfo

@Composable
fun CategoriesScreen(
    container: AppContainer,
    onOpenBrowse: (slug: String, name: String) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
            .padding(48.dp)
    ) {
        Text(text = "Kategoriler", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(GENRES, key = { it.slug }) { g ->
                GenreCard(g, onClick = { onOpenBrowse(g.slug, g.name) })
            }
        }
    }
}

@Composable
private fun GenreCard(g: GenreInfo, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .height(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2B0A0A), Color(0xFF15151E))
                )
            )
            .border(
                if (focused) 3.dp else 0.dp,
                Color(0xFFE53935),
                RoundedCornerShape(14.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown &&
                    (ev.key == Key.Enter || ev.key == Key.DirectionCenter || ev.key == Key.NumPadEnter)
                ) {
                    onClick(); true
                } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = g.name,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
