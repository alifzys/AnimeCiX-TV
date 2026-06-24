package com.alifzys.an1mecix.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

private const val SKIP_COUNTDOWN = 10  // geri sayım (sn)

/**
 * Opening/Ending atlama bandı: sağ-alt köşede "Atla" + iptal butonu, 10 sn geri sayımlı.
 * Geri sayım biterse otomatik olarak [onSkipNow] tetiklenir.
 */
@Composable
internal fun SkipPrompt(
    label: String,
    onSkipNow: () -> Unit,
    onDismiss: () -> Unit,
) {
    val skipFocus = remember { FocusRequester() }
    var remaining by remember { mutableStateOf(SKIP_COUNTDOWN) }

    LaunchedEffect(Unit) {
        runCatching { skipFocus.requestFocus() }
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
        onSkipNow()  // geri sayım bitti → otomatik atla
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(end = 40.dp, bottom = 40.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color(0xE6101018), RoundedCornerShape(14.dp))
                .focusGroup()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            // "Atla" butonu — geri sayımlı
            Surface(
                onClick = onSkipNow,
                modifier = Modifier.focusRequester(skipFocus),
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Color.Black,
                ),
            ) {
                Text(
                    text = "▶  $label  ($remaining)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            // İptal (✕)
            Surface(
                onClick = onDismiss,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(10.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color(0x33FFFFFF),
                    contentColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedContentColor = Color.Black,
                ),
            ) {
                Text(
                    text = "✕",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                )
            }
        }
    }
}
