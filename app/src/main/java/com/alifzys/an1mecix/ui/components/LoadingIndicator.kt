package com.alifzys.an1mecix.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/**
 * Belirsiz (indeterminate) yükleme çubuğu — düz "Yükleniyor..." yazısı yerine.
 * Kırmızı segment ray üzerinde sağa sola kayar. Material bağımlılığı yok.
 */
@Composable
fun LoadingBar(
    label: String? = null,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "loading")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "slide",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(180.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            // Kayan segment: rayın %35'i kadar, soldan sağa geçer
            Box(
                Modifier
                    .fillMaxWidth(0.35f)
                    .height(3.dp)
                    .graphicsLayer {
                        // Segment rayın solundan (-0.35w) sağ dışına (1.0w) kayar
                        translationX = (progress * 1.35f - 0.35f) * size.width
                    }
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE53935)),
            )
        }
        if (label != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** Tüm ekranı kaplayan ortalanmış yükleme çubuğu. */
@Composable
fun FullScreenLoading(label: String? = null) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        LoadingBar(label = label)
    }
}
