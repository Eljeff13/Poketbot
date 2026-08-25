package com.eljeff13.poketbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.eljeff13.poketbot.ui.PoketbotApp
import com.eljeff13.poketbot.ui.theme.PoketbotTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PoketbotTheme {
                PoketbotApp()
            }
        }
    }
}
