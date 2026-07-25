package com.example.alphamusic.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.alphamusic.core.domain.models.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val coverUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localUri: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

fun TrackEntity.toDomain() = Track(
    id = id,
    title = title,
    artistName = artistName,
    albumName = albumName,
    coverUrl = coverUrl,
    streamUrl = localUri ?: streamUrl,
    durationMs = durationMs,
    localUri = localUri
)

fun Track.toEntity(isLiked: Boolean = false, isDownloaded: Boolean = false, localUri: String? = this.localUri) = TrackEntity(
    id = id,
    title = title,
    artistName = artistName,
    albumName = albumName,
    coverUrl = coverUrl,
    streamUrl = streamUrl,
    durationMs = durationMs,
    isLiked = isLiked,
    isDownloaded = isDownloaded,
    localUri = localUri
)
