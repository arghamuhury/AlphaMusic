package com.example.alphamusic.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences("alpha_music_settings", Context.MODE_PRIVATE)

    var highQualityAudio: Boolean
        get() = preferences.getBoolean(KEY_HIGH_QUALITY_AUDIO, true)
        set(value) = preferences.edit().putBoolean(KEY_HIGH_QUALITY_AUDIO, value).apply()

    var downloadWifiOnly: Boolean
        get() = preferences.getBoolean(KEY_DOWNLOAD_WIFI_ONLY, true)
        set(value) = preferences.edit().putBoolean(KEY_DOWNLOAD_WIFI_ONLY, value).apply()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)

    init {
        _themeMode.value = readThemeMode()
    }

    var themeMode: ThemeMode
        get() = _themeMode.value
        set(value) {
            _themeMode.value = value
            preferences.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    val themeModeFlow: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private fun readThemeMode(): ThemeMode {
        val name = preferences.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try { ThemeMode.valueOf(name) } catch (_: Exception) { ThemeMode.SYSTEM }
    }

    private companion object {
        const val KEY_HIGH_QUALITY_AUDIO = "high_quality_audio"
        const val KEY_DOWNLOAD_WIFI_ONLY = "download_wifi_only"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
