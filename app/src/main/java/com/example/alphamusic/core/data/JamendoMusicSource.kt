package com.example.alphamusic.core.data

import com.example.alphamusic.core.data.remote.JamendoApiService
import com.example.alphamusic.core.domain.models.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JamendoMusicSource @Inject constructor(
    private val apiService: JamendoApiService
) : MusicSource {

    private val clientId = "56d30c95" // Default public demo key for Jamendo

    override suspend fun searchTracks(query: String): Result<List<Track>> {
        return try {
            val response = apiService.getTracks(clientId = clientId, search = query)
            Result.success(response.results.map {
                Track(
                    id = it.id,
                    title = it.name,
                    artistName = it.artist_name,
                    albumName = it.album_name ?: "Unknown Album",
                    coverUrl = it.image,
                    streamUrl = it.audio,
                    durationMs = it.duration * 1000L // Jamendo returns seconds
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrendingTracks(): Result<List<Track>> {
        return try {
            val response = apiService.getTracks(clientId = clientId, boost = "popularity_week")
            Result.success(response.results.map {
                Track(
                    id = it.id,
                    title = it.name,
                    artistName = it.artist_name,
                    albumName = it.album_name ?: "Unknown Album",
                    coverUrl = it.image,
                    streamUrl = it.audio,
                    durationMs = it.duration * 1000L
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
