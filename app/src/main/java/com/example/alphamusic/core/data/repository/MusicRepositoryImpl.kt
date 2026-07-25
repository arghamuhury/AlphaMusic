package com.example.alphamusic.core.data.repository

import com.example.alphamusic.core.data.DownloadState
import com.example.alphamusic.core.data.MusicSource
import com.example.alphamusic.core.data.local.DownloadStorage
import com.example.alphamusic.core.data.local.TrackDao
import com.example.alphamusic.core.data.local.toDomain
import com.example.alphamusic.core.data.local.toEntity
import com.example.alphamusic.core.domain.MusicRepository
import com.example.alphamusic.core.domain.models.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val remoteSource: MusicSource,
    private val trackDao: TrackDao,
    private val playlistDao: com.example.alphamusic.core.data.local.PlaylistDao,
    private val downloadStorage: DownloadStorage
) : MusicRepository {

    private val downloadProgressMap = ConcurrentHashMap<String, MutableStateFlow<DownloadState>>()

    override fun getDownloadProgress(trackId: String): StateFlow<DownloadState> {
        return downloadProgressMap.getOrPut(trackId) { MutableStateFlow(DownloadState.Idle) }.asStateFlow()
    }

    override suspend fun searchTracks(query: String): Result<List<Track>> {
        return remoteSource.searchTracks(query)
    }

    override suspend fun getTrendingTracks(): Result<List<Track>> {
        return remoteSource.getTrendingTracks()
    }

    override fun getLikedTracks(): Flow<List<Track>> {
        return trackDao.getLikedTracks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun toggleLikeStatus(track: Track, isLiked: Boolean) {
        trackDao.insertTrackIgnore(track.toEntity(isLiked = isLiked, isDownloaded = false))
        trackDao.updateLikedStatus(track.id, isLiked)
    }

    override fun getDownloadedTracks(): Flow<List<Track>> {
        return trackDao.getDownloadedTracks().map { entities ->
            entities
                .filter { downloadStorage.hasDownloadedFile(it.localUri) }
                .map { it.toDomain() }
        }
    }

    override suspend fun toggleDownloadStatus(track: Track, isDownloaded: Boolean) {
        trackDao.insertTrackIgnore(track.toEntity(isLiked = false, isDownloaded = false))
        if (isDownloaded) {
            val progressFlow = downloadProgressMap.getOrPut(track.id) { MutableStateFlow(DownloadState.Idle) }
            progressFlow.value = DownloadState.Downloading(0f)

            val result = downloadStorage.downloadTrack(track) { progress ->
                progressFlow.value = DownloadState.Downloading(progress)
            }

            result.onSuccess { localUri ->
                trackDao.updateDownloadStatus(track.id, isDownloaded = true, localUri = localUri)
                progressFlow.value = DownloadState.Downloaded
            }.onFailure {
                progressFlow.value = DownloadState.Idle
            }
        } else {
            downloadStorage.removeTrack(track)
            trackDao.updateDownloadStatus(track.id, isDownloaded = false, localUri = null)
            downloadProgressMap[track.id]?.value = DownloadState.Idle
        }
    }

    override suspend fun removeAllDownloads() {
        downloadStorage.removeAllDownloads()
        trackDao.clearDownloadedTracks()
    }

    override suspend fun clearCache(): Long {
        return downloadStorage.clearCache()
    }

    override suspend fun getDownloadsSizeBytes(): Long {
        return downloadStorage.downloadsSizeBytes()
    }

    override suspend fun getCacheSizeBytes(): Long {
        return downloadStorage.cacheSizeBytes()
    }

    override fun getAllPlaylists(): Flow<List<com.example.alphamusic.core.data.local.PlaylistEntity>> {
        return playlistDao.getAllPlaylists()
    }

    override fun getTracksForPlaylist(playlistId: String): Flow<List<Track>> {
        return playlistDao.getTracksForPlaylist(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createPlaylist(name: String): String {
        val id = java.util.UUID.randomUUID().toString()
        val playlist = com.example.alphamusic.core.data.local.PlaylistEntity(
            id = id,
            name = name,
            createdAt = System.currentTimeMillis()
        )
        playlistDao.insertPlaylist(playlist)
        return id
    }

    override suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        trackDao.insertTrackIgnore(track.toEntity())

        val crossRef = com.example.alphamusic.core.data.local.PlaylistTrackCrossRef(
            playlistId = playlistId,
            trackId = track.id,
            addedAt = System.currentTimeMillis()
        )
        playlistDao.insertPlaylistTrackCrossRef(crossRef)
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    override suspend fun deletePlaylist(playlistId: String) {
        playlistDao.deletePlaylist(playlistId)
    }
}