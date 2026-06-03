package com.alifzys.an1mecix.ui.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.alifzys.an1mecix.domain.model.AnimeCard

@Composable
fun PosterRow(
    title: String,
    items: List<AnimeCard>,
    onOpen: (AnimeCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.padding(vertical = 10.dp)) {
        Text(
            text = title.uppercase(),
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 48.dp, bottom = 12.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .focusGroup(),
        ) {
            items(items, key = { it.id }) { card ->
                PosterCard(item = card, onClick = { onOpen(card) })
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}
