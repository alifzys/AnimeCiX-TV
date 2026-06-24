package com.alifzys.an1mecix.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.alifzys.an1mecix.domain.model.AnimeCard

@Composable
fun PosterCard(
    item: AnimeCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 160.dp,
    height: Dp = 240.dp,
) {
    var focused by remember { mutableStateOf(false) }
    // Gecikme kapısı yok (focus tepkisi anında), ama animasyon daha uzun/yumuşak
    // sürede oynar → hızlı d-pad gezinmede "kasma/zıplama" hissi azalır.
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "scale",
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (focused) 0.9f else 0f,
        animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "ring",
    )

    val ctx = LocalContext.current
    val density = LocalDensity.current
    // px boyutu ve ImageRequest sabit — her recomposition'da yeniden allocate etme
    val request = remember(item.poster, width, height) {
        val pxW = with(density) { width.toPx().toInt() }
        val pxH = with(density) { height.toPx().toInt() }
        ImageRequest.Builder(ctx)
            .data(item.poster)
            .size(pxW, pxH)
            .scale(Scale.FILL)
            .build()
    }

    Column(
        modifier = modifier
            .width(width)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .onKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyDown &&
                    (ev.key == Key.Enter || ev.key == Key.DirectionCenter || ev.key == Key.NumPadEnter)
                ) {
                    onClick(); true
                } else false
            }
    ) {
        Box(
            Modifier
                .width(width)
                .height(height)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1C1C1E))
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = ringAlpha),
                    shape = RoundedCornerShape(10.dp),
                )
        ) {
            if (!item.poster.isNullOrBlank()) {
                AsyncImage(
                    model = request,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            if (item.rating != null && item.rating > 0) {
                Box(
                    Modifier
                        .align(androidx.compose.ui.Alignment.TopStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "★ ${"%.1f".format(item.rating)}",
                        color = Color(0xFFFFCC00),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(
            text = item.name,
            color = Color.White.copy(alpha = if (focused) 1f else 0.55f),
            fontSize = 12.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .width(width)
                .padding(top = 7.dp, start = 2.dp, end = 2.dp),
        )
    }
}
