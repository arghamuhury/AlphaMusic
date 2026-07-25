package com.example.alphamusic.core.data

import com.example.alphamusic.core.domain.models.Track

interface MusicSource {
    suspend fun searchTracks(query: String): Result<List<Track>>
    suspend fun getTrendingTracks(): Result<List<Track>>
}
