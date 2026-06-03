package com.alifzys.an1mecix

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
import com.alifzys.an1mecix.ui.navigation.AppNavHost
import com.alifzys.an1mecix.ui.theme.AnimeCixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AnimeCixTheme {
                val container = (application as AnimeCixApp).container
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0F))
                ) {
                    AppNavHost(container = container)
                }
            }
        }
        // setContent sonrası decor view hazır — system bar'ları gizle
        WindowCompat.getInsetsController(window, window.decorView).hide(
            WindowInsetsCompat.Type.systemBars()
        )
    }
}
