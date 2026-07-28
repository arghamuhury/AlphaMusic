package com.example.alphamusic.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphamusic.core.data.AppSettings
import com.example.alphamusic.core.data.ThemeMode
import com.example.alphamusic.core.domain.MusicRepository
import com.example.alphamusic.core.player.SleepTimerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val appSettings: AppSettings,
    private val sleepTimerManager: SleepTimerManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                highQualityAudio = appSettings.highQualityAudio,
                downloadWifiOnly = appSettings.downloadWifiOnly,
                themeMode = appSettings.themeMode
            )
        }
        refreshStorageInfo()
    }

    fun setHighQualityAudio(enabled: Boolean) {
        appSettings.highQualityAudio = enabled
        _uiState.update { it.copy(highQualityAudio = enabled) }
    }

    fun setDownloadWifiOnly(enabled: Boolean) {
        appSettings.downloadWifiOnly = enabled
        _uiState.update { it.copy(downloadWifiOnly = enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        appSettings.themeMode = mode
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun startSleepTimer() {
        sleepTimerManager.start()
    }

    fun clearCache() {
        viewModelScope.launch {
            runCatching { repository.clearCache() }
                .onSuccess { refreshStorageInfo() }
        }
    }

    fun removeAllDownloads() {
        viewModelScope.launch {
            runCatching { repository.removeAllDownloads() }
                .onSuccess { refreshStorageInfo() }
        }
    }

    private fun refreshStorageInfo() {
        viewModelScope.launch {
            val cacheSize = runCatching { repository.getCacheSizeBytes() }.getOrDefault(0L)
            val downloadsSize = runCatching { repository.getDownloadsSizeBytes() }.getOrDefault(0L)
            _uiState.update {
                it.copy(
                    cacheSizeLabel = formatBytes(cacheSize),
                    downloadsSizeLabel = formatBytes(downloadsSize)
                )
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 bytes"
        val units = listOf("bytes", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex++
        }
        return if (unitIndex == 0) {
            "${value.toLong()} ${units[unitIndex]}"
        } else {
            "${String.format("%.1f", value)} ${units[unitIndex]}"
        }
    }
}

data class SettingsUiState(
    val highQualityAudio: Boolean = true,
    val downloadWifiOnly: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val cacheSizeLabel: String = "0 bytes",
    val downloadsSizeLabel: String = "0 bytes"
)
