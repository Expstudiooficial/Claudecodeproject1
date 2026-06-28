package com.expstudio.localai.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.expstudio.localai.ui.nav.LocalAiNavHost
import com.expstudio.localai.ui.theme.LocalAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalAITheme {
                LocalAiNavHost()
            }
        }
    }
}
