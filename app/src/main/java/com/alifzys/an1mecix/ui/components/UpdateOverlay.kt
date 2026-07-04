package com.alifzys.an1mecix.ui.components

import android.view.KeyEvent as AKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.alifzys.an1mecix.data.update.UpdateInfo
import com.alifzys.an1mecix.data.update.UpdateService
import java.io.File

private sealed interface UpdateUi {
    data object Idle : UpdateUi
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateUi
    data class Installing(val info: UpdateInfo) : UpdateUi
}

/**
 * Açılışta sessizce güncelleme kontrolü yapar. Yeni sürüm varsa otomatik indirir ve
 * sistem kurulum akışını başlatır. İndirme/kurulum sırasında ekranı TAM bloke eder
 * (D-pad ve dokunma arkaya geçmez) — kullanıcı arkada bir şey yapamaz. Güncelleme
 * yoksa hiçbir şey göstermez.
 */
@Composable
fun UpdateOverlay() {
    val context = LocalContext.current
    var ui by remember { mutableStateOf<UpdateUi>(UpdateUi.Idle) }

    // Kurulum sonucu (MainActivity → PackageInstaller.STATUS_*). Onay ekranı beklerken
    // (PENDING_USER_ACTION=-1) veya başarıda (SUCCESS=0) bekleriz; hata/iptalde (>0)
    // bloklamayı kaldırıp overlay'i kapatırız ki kullanıcı takılıp kalmasın.
    DisposableEffect(Unit) {
        UpdateService.installStatusListener = { status ->
            if (status > 0) ui = UpdateUi.Idle
        }
        onDispose { UpdateService.installStatusListener = null }
    }

    LaunchedEffect(Unit) {
        val info = UpdateService.check() ?: return@LaunchedEffect
        ui = UpdateUi.Downloading(info, 0f)
        val apk = File(context.cacheDir, "animecix-update.apk")
        runCatching {
            UpdateService.download(info.downloadUrl, apk) { p ->
                ui = UpdateUi.Downloading(info, p)
            }
        }.onSuccess {
            ui = UpdateUi.Installing(info)
            runCatching { UpdateService.install(context, apk) }
                .onFailure { ui = UpdateUi.Idle }
        }.onFailure {
            ui = UpdateUi.Idle
        }
    }

    when (val s = ui) {
        is UpdateUi.Downloading -> BlockingModal(dismissible = false, onDismiss = {}) {
            UpdateCard(
                title = "Güncelleme bulundu  •  v${s.info.versionName}",
                subtitle = "İndiriliyor…  %${(s.progress * 100).toInt()}",
                progress = s.progress,
            )
        }
        is UpdateUi.Installing -> BlockingModal(
            dismissible = true,
            onDismiss = { ui = UpdateUi.Idle },
        ) {
            UpdateCard(
                title = "Güncelleme  •  v${s.info.versionName}",
                subtitle = "Sistem kurulum ekranını onaylayın…",
                progress = null,
            )
        }
        UpdateUi.Idle -> Unit
    }
}

/**
 * Tam ekran bloklayıcı katman: odağı yakalar, tüm D-pad tuşlarını ve dokunmayı yutar
 * (BACK hariç → BackHandler'a bırakılır). [dismissible] true ise BACK overlay'i kapatır.
 */
@Composable
private fun BlockingModal(
    dismissible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BackHandler(enabled = true) { if (dismissible) onDismiss() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xE6000000))
            .focusRequester(focus)
            .focusable()
            // BACK dışındaki tüm tuşları yut → arka plandaki menü hareket etmesin.
            .onKeyEvent { ev -> ev.nativeKeyEvent.keyCode != AKeyEvent.KEYCODE_BACK }
            // Dokunmayı da yut (dokunmatik TV/kutu).
            .pointerInput(Unit) {
                awaitPointerEventScope { while (true) awaitPointerEvent() }
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun UpdateCard(title: String, subtitle: String, progress: Float?) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.32f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF15151C))
            .padding(horizontal = 32.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(20.dp))
        // İlerleme çubuğu (indeterminate ise dolu göster)
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress ?: 1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFE53935)),
            )
        }
    }
}
