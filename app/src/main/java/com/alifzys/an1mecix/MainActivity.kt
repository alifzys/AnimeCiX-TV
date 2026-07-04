package com.alifzys.an1mecix

import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.alifzys.an1mecix.data.update.UpdateService
import com.alifzys.an1mecix.ui.components.UpdateOverlay
import com.alifzys.an1mecix.ui.navigation.AppNavHost
import com.alifzys.an1mecix.ui.theme.AnimeCixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handleInstallResult(intent)
        setContent {
            AnimeCixTheme {
                val container = (application as AnimeCixApp).container
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0F))
                ) {
                    AppNavHost(container = container)
                    // Açılışta otomatik güncelleme kontrolü (varsa indir + kur)
                    UpdateOverlay()
                }
            }
        }
        // setContent sonrası decor view hazır — system bar'ları gizle
        WindowCompat.getInsetsController(window, window.decorView).hide(
            WindowInsetsCompat.Type.systemBars()
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleInstallResult(intent)
    }

    /**
     * PackageInstaller callback'i. STATUS_PENDING_USER_ACTION gelince sistemin
     * "bu uygulamayı güncelle?" onay ekranını BAŞLATMAK zorundayız — yoksa kurulum
     * "Kuruluyor…" durumunda asılı kalır. Diğer sonuçları overlay'e iletiriz.
     */
    private fun handleInstallResult(intent: Intent?) {
        if (intent?.action != UpdateService.INSTALL_ACTION) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, Int.MIN_VALUE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_INTENT)
            }
            confirm?.let { runCatching { startActivity(it) } }
        }
        UpdateService.installStatusListener?.invoke(status)
    }
}
