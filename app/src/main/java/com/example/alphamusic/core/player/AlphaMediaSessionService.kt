package com.example.alphamusic.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlphaMediaSessionService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    @Inject
    lateinit var playbackStateManager: PlaybackStateManager

    @Inject
    lateinit var musicRepository: com.example.alphamusic.core.domain.MusicRepository

    private lateinit var exoPlayer: ExoPlayer
    private var progressJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
            
        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            
        val sessionActivityPendingIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            ?.let { launchIntent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .apply {
                sessionActivityPendingIntent?.let(::setSessionActivity)
            }
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playbackStateManager.updateIsPlaying(isPlaying)
                if (isPlaying) {
                    startTrackingProgress()
                } else {
                    stopTrackingProgress()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                playbackStateManager.updateDuration(exoPlayer.duration.coerceAtLeast(0))
                playbackStateManager.updatePlaybackPosition(exoPlayer.currentPosition.coerceAtLeast(0))
                
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                    mediaItem?.let { item ->
                        val metadata = item.mediaMetadata
                        val track = com.example.alphamusic.core.domain.models.Track(
                            id = item.mediaId,
                            title = metadata.title?.toString() ?: "",
                            artistName = metadata.artist?.toString() ?: "",
                            albumName = metadata.albumTitle?.toString() ?: "",
                            coverUrl = metadata.artworkUri?.toString() ?: "",
                            streamUrl = item.localConfiguration?.uri?.toString() ?: "",
                            durationMs = 0L // Handled by duration flow
                        )
                        playbackStateManager.updateCurrentTrack(track)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_ENDED) {
                    // Autoplay next song logic
                    serviceScope.launch {
                        musicRepository.getTrendingTracks().onSuccess { tracks ->
                            val currentTrackId = exoPlayer.currentMediaItem?.mediaId
                            val nextTrack = tracks.filter { it.id != currentTrackId }.randomOrNull()
                            
                            nextTrack?.let { track ->
                                val metadata = MediaMetadata.Builder()
                                    .setTitle(track.title)
                                    .setArtist(track.artistName)
                                    .setAlbumTitle(track.albumName)
                                    .setArtworkUri(android.net.Uri.parse(track.coverUrl))
                                    .build()
                                    
                                val newMediaItem = MediaItem.Builder()
                                    .setMediaId(track.id)
                                    .setUri(track.streamUrl)
                                    .setMediaMetadata(metadata)
                                    .build()
                                    
                                playbackStateManager.updateCurrentTrack(track)
                                exoPlayer.setMediaItem(newMediaItem)
                                exoPlayer.prepare()
                                exoPlayer.play()
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player?.playWhenReady == true || player?.isPlaying == true) {
            player?.pause()
            player?.stop()
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopTrackingProgress()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
    
    private fun startTrackingProgress() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                playbackStateManager.updatePlaybackPosition(exoPlayer.currentPosition.coerceAtLeast(0))
                playbackStateManager.updateDuration(exoPlayer.duration.coerceAtLeast(0))
                delay(1000L) // Update every second
            }
        }
    }
    
    private fun stopTrackingProgress() {
        progressJob?.cancel()
        progressJob = null
    }
}
