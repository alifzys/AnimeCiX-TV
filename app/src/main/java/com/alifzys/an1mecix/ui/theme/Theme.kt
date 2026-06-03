package com.alifzys.an1mecix.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val AnimeCixDarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFE53935),       // animecix kırmızı
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = androidx.compose.ui.graphics.Color(0xFFFFAB91),
    background = androidx.compose.ui.graphics.Color(0xFF0A0A0F),
    onBackground = androidx.compose.ui.graphics.Color.White,
    surface = androidx.compose.ui.graphics.Color(0xFF15151E),
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1E1E2A),
)

@Composable
fun AnimeCixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AnimeCixDarkColors,
        content = content,
    )
}
