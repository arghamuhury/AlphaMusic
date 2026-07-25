package com.example.alphamusic.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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

    private companion object {
        const val KEY_HIGH_QUALITY_AUDIO = "high_quality_audio"
        const val KEY_DOWNLOAD_WIFI_ONLY = "download_wifi_only"
    }
}
