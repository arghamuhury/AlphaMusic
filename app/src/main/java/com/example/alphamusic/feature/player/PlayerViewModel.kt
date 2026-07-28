package com.example.alphamusic.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphamusic.core.data.DownloadState
import com.example.alphamusic.core.data.LyricLine
import com.example.alphamusic.core.data.LyricsSource
import com.example.alphamusic.core.data.local.PlaylistEntity
import com.example.alphamusic.core.domain.models.Track
import com.example.alphamusic.core.player.MusicController
import com.example.alphamusic.core.player.PlaybackStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicController: MusicController,
    private val playbackStateManager: PlaybackStateManager,
    private val repository: com.example.alphamusic.core.domain.MusicRepository,
    private val lyricsSource: LyricsSource
) : ViewModel() {

    val currentTrack: StateFlow<Track?> = playbackStateManager.currentTrack
    val isPlaying: StateFlow<Boolean> = playbackStateManager.isPlaying
    val playbackPosition: StateFlow<Long> = playbackStateManager.playbackPosition
    val duration: StateFlow<Long> = playbackStateManager.duration
    val repeatMode: StateFlow<Int> = playbackStateManager.repeatMode
    // Using stateIn to collect latest from database
    val likedTracks = repository.getLikedTracks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Track>())
    val downloadedTracks = repository.getDownloadedTracks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Track>())
    val playlists = repository.getAllPlaylists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<PlaylistEntity>())

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    init {
        musicController.initialize()
        observeTrackChanges()
    }

    private fun observeTrackChanges() {
        viewModelScope.launch {
            playbackStateManager.currentTrack.collect { track ->
                track?.let { loadLyrics(it) }
            }
        }
    }

    private suspend fun loadLyrics(track: Track) {
        _isLyricsLoading.value = true
        _lyrics.value = emptyList()
        val result = lyricsSource.fetchLyrics(track.artistName, track.title)
        if (result != null) {
            _lyrics.value = result
        }
        _isLyricsLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        musicController.release()
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            musicController.pause()
        } else {
            musicController.resume()
        }
    }

    fun toggleRepeatMode() {
        musicController.toggleRepeatMode()
    }

    fun playTrack(track: Track) {
        musicController.playTrack(track)
    }

    fun playNext(track: Track) {
        musicController.playNext(track)
    }

    fun seekTo(position: Long) {
        musicController.seekTo(position)
    }

    fun skipToNext() {
        musicController.skipToNext()
    }

    fun skipToPrevious() {
        musicController.skipToPrevious()
    }

    fun toggleLikeStatus(track: Track, isLiked: Boolean) {
        viewModelScope.launch {
            repository.toggleLikeStatus(track, isLiked)
        }
    }

    fun toggleDownloadStatus(track: Track, isDownloaded: Boolean) {
        viewModelScope.launch {
            repository.toggleDownloadStatus(track, isDownloaded)
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            repository.getDownloadProgress(track.id).collect { state ->
                _downloadStates.update { it + (track.id to state) }
            }
        }
        viewModelScope.launch {
            repository.toggleDownloadStatus(track, true)
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }
}
