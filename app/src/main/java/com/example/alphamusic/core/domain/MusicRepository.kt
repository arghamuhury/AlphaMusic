package com.example.alphamusic.core.domain

import com.example.alphamusic.core.data.DownloadState
import com.example.alphamusic.core.domain.models.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MusicRepository {
    suspend fun searchTracks(query: String): Result<List<Track>>
    suspend fun getTrendingTracks(): Result<List<Track>>
    fun getLikedTracks(): Flow<List<Track>>
    suspend fun toggleLikeStatus(track: Track, isLiked: Boolean)

    // Downloads
    fun getDownloadedTracks(): Flow<List<Track>>
    suspend fun toggleDownloadStatus(track: Track, isDownloaded: Boolean)
    fun getDownloadProgress(trackId: String): StateFlow<DownloadState>
    suspend fun removeAllDownloads()
    suspend fun clearCache(): Long
    suspend fun getDownloadsSizeBytes(): Long
    suspend fun getCacheSizeBytes(): Long

    // Playlists
    fun getAllPlaylists(): Flow<List<com.example.alphamusic.core.data.local.PlaylistEntity>>
    fun getTracksForPlaylist(playlistId: String): Flow<List<Track>>
    suspend fun createPlaylist(name: String): String
    suspend fun addTrackToPlaylist(playlistId: String, track: Track)
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
    suspend fun deletePlaylist(playlistId: String)
}
