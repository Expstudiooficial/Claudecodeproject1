package com.expstudio.localai.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.expstudio.localai.LocalAiApp
import com.expstudio.localai.data.settings.AppTheme
import com.expstudio.localai.ui.nav.LocalAiNavHost
import com.expstudio.localai.ui.theme.LocalAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = (application as LocalAiApp).container.settingsStore
        setContent {
            val settings by store.settings.collectAsState()
            val dark = when (settings.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }
            LocalAITheme(darkTheme = dark, dynamicColor = settings.dynamicColor) {
                LocalAiNavHost()
            }
        }
    }
}
