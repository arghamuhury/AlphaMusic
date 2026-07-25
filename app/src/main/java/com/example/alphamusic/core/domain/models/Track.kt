package com.example.alphamusic.core.domain.models

data class Track(
    val id: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val coverUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val localUri: String? = null
)
