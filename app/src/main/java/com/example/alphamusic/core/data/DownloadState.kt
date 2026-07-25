package com.example.alphamusic.core.data

sealed interface DownloadState {
    data object Idle : DownloadState
    data object Downloaded : DownloadState
    data class Downloading(val progress: Float) : DownloadState // 0f..1f
}