package com.example.alphamusic.core.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.alphamusic.core.domain.models.Track
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackStateManager: PlaybackStateManager
) {
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val pendingControllerActions = ArrayDeque<MediaController.() -> Unit>()

    fun initialize() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, AlphaMediaSessionService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.get()
                flushPendingControllerActions()
            },
            MoreExecutors.directExecutor()
        )
    }

    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
        pendingControllerActions.clear()
    }

    fun playTrack(track: Track) {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artistName)
            .setAlbumTitle(track.albumName)
            .setArtworkUri(android.net.Uri.parse(track.coverUrl))
            .build()
            
        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.streamUrl)
            .setMediaMetadata(metadata)
            .build()
            
        playbackStateManager.updateCurrentTrack(track)
        
        withController {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }

    fun playNext(track: Track) {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artistName)
            .setAlbumTitle(track.albumName)
            .setArtworkUri(android.net.Uri.parse(track.coverUrl))
            .build()
            
        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.streamUrl)
            .setMediaMetadata(metadata)
            .build()
            
        withController {
            if (mediaItemCount == 0) {
                playbackStateManager.updateCurrentTrack(track)
                setMediaItem(mediaItem)
                prepare()
                play()
            } else {
                val nextIndex = if (currentMediaItemIndex != androidx.media3.common.C.INDEX_UNSET) {
                    currentMediaItemIndex + 1
                } else {
                    mediaItemCount
                }
                addMediaItem(nextIndex, mediaItem)
            }
        }
    }

    fun pause() {
        withController { pause() }
    }

    fun resume() {
        withController { play() }
    }

    fun seekTo(position: Long) {
        withController { seekTo(position) }
        playbackStateManager.updatePlaybackPosition(position)
    }

    fun toggleRepeatMode() {
        val currentMode = mediaController?.repeatMode ?: androidx.media3.common.Player.REPEAT_MODE_OFF
        val nextMode = if (currentMode == androidx.media3.common.Player.REPEAT_MODE_OFF) {
            androidx.media3.common.Player.REPEAT_MODE_ONE
        } else {
            androidx.media3.common.Player.REPEAT_MODE_OFF
        }
        withController { repeatMode = nextMode }
        playbackStateManager.updateRepeatMode(nextMode)
    }

    fun skipToNext() {
        // Seek to end to trigger STATE_ENDED autoplay logic
        withController {
            val duration = duration
            if (duration > 0) {
                seekTo(duration)
            }
        }
    }

    fun skipToPrevious() {
        // Restart current track for now
        withController { seekTo(0) }
    }

    private fun withController(action: MediaController.() -> Unit) {
        val controller = mediaController
        if (controller != null) {
            controller.action()
        } else {
            pendingControllerActions.addLast(action)
            initialize()
        }
    }

    private fun flushPendingControllerActions() {
        val controller = mediaController ?: return
        while (pendingControllerActions.isNotEmpty()) {
            pendingControllerActions.removeFirst().invoke(controller)
        }
    }
}
