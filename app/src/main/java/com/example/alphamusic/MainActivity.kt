package com.example.alphamusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.alphamusic.core.data.AppSettings
import com.example.alphamusic.core.data.ThemeMode
import com.example.alphamusic.core.ui.theme.AlphaMusicTheme
import com.example.alphamusic.feature.main.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appSettings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by appSettings.themeModeFlow.collectAsState()
            val currentIsSystemDark = isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> currentIsSystemDark
            }
            AlphaMusicTheme(isDarkTheme = isDarkTheme) {
                MainScreen()
            }
        }
    }
}
