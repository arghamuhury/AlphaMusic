package com.example.alphamusic.core.data

import kotlinx.serialization.Serializable

@Serializable
data class LyricsResponse(
    val id: Long? = null,
    val name: String? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

data class LyricLine(
    val timestampMs: Long,
    val text: String
)
