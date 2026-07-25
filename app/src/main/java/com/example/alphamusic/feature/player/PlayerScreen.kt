package com.example.alphamusic.feature.player

import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.alphamusic.core.data.DownloadState
import com.example.alphamusic.core.domain.models.Track
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.abs

// Removed local DownloadState enum — using core.data.DownloadState

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    track: Track?,
    isPlaying: Boolean,
    playbackPosition: Long,
    duration: Long,
    repeatMode: Int,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onRepeatToggle: () -> Unit,
    onLikeToggle: (Boolean) -> Unit,
    onDownloadToggle: (Boolean) -> Unit,
    onAddToPlaylist: (String, Track) -> Unit,
    isLiked: Boolean,
    isDownloaded: Boolean,
    downloadStates: Map<String, DownloadState>,
    playlists: List<com.example.alphamusic.core.data.local.PlaylistEntity>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (track == null) return

    val view = LocalView.current
    val performHaptic = remember { { type: Int -> view.performHapticFeedback(type) } }

    var isOptionsSheetVisible by remember { mutableStateOf(false) }
    var isPlaylistSheetVisible by remember { mutableStateOf(false) }

    val downloadState = downloadStates[track.id] ?: (if (isDownloaded) DownloadState.Downloaded else DownloadState.Idle)

    val scope = rememberCoroutineScope()
    var dragPosition by remember { mutableStateOf<Float?>(null) }

    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "LikeScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        // Layer 3: Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            BackHandler { onClose() }
            
            // 3.1 Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.6f)
                )

                IconButton(onClick = { isOptionsSheetVisible = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.9f))

            // 3.3 Album Art Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 100f) onClose()
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (abs(dragAmount) > 50f) {
                                performHaptic(HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                    }
            ) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = "Album Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 3.5 Song Info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = track.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

            // 3.7 Progress Slider
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentPosition = dragPosition?.toLong() ?: playbackPosition
                val fraction = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                val infiniteTransition = rememberInfiniteTransition(label = "wave")
                val phase by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = (2 * Math.PI).toFloat(),
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "phase"
                )
                val currentPhase = if (isPlaying) phase else 0f

                Canvas(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
                    val midY = size.height / 2f
                    val activeWidth = size.width * fraction

                    val path = Path()
                    val waveLength = 30.dp.toPx()
                    val amplitude = 4.dp.toPx()
                    val step = 2.dp.toPx()
                    val B = (2 * Math.PI / waveLength).toFloat()
                    
                    val initialY = midY + amplitude * kotlin.math.sin(B * 0f - currentPhase)
                    path.moveTo(0f, initialY)
                    
                    var currentX = 0f
                    while (currentX < activeWidth) {
                        currentX += step
                        if (currentX > activeWidth) currentX = activeWidth
                        val currentY = midY + amplitude * kotlin.math.sin(B * currentX - currentPhase)
                        path.lineTo(currentX, currentY)
                    }

                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(activeWidth, midY),
                        end = Offset(size.width, midY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                Slider(
                    value = if (duration > 0) currentPosition.toFloat() else 0f,
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    onValueChange = { dragPosition = it },
                    onValueChangeFinished = {
                        dragPosition?.let { pos ->
                            onSeekTo(pos.toLong())
                            scope.launch {
                                delay(400)
                                dragPosition = null
                            }
                        }
                    },
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Time indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentPositionSafe = dragPosition?.toLong() ?: playbackPosition
                Text(
                    text = formatTime(currentPositionSafe),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = "-" + formatTime(duration - currentPositionSafe),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3.9 Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        onLikeToggle(!isLiked)
                        performHaptic(HapticFeedbackConstants.CONFIRM)
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) Color.White else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.scale(likeScale)
                    )
                }
                IconButton(onClick = { performHaptic(HapticFeedbackConstants.CLOCK_TICK) }) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { 
                            performHaptic(HapticFeedbackConstants.CONFIRM)
                            onPlayPauseClick() 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            (scaleIn(initialScale = 0.6f) + fadeIn()).togetherWith(scaleOut(targetScale = 0.6f) + fadeOut())
                        },
                        label = "playPauseAnimation"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                IconButton(onClick = { performHaptic(HapticFeedbackConstants.CLOCK_TICK) }) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { 
                    performHaptic(HapticFeedbackConstants.LONG_PRESS)
                    onRepeatToggle()
                }) {
                    Icon(
                        Icons.Rounded.Repeat, 
                        contentDescription = "Repeat", 
                        tint = if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) Color.White else Color.White.copy(alpha = 0.4f)
                    )
                }
            }
            }
            
            Spacer(modifier = Modifier.weight(1.2f))
        }
    }

    if (isOptionsSheetVisible) {
        PlayerOptionsSheet(
            track = track,
            downloadState = downloadState,
            onDismiss = { isOptionsSheetVisible = false },
            onDownloadClick = {
                performHaptic(HapticFeedbackConstants.CONFIRM)
                onDownloadToggle(!isDownloaded)
            },
            onAddToPlaylistClick = {
                isOptionsSheetVisible = false
                isPlaylistSheetVisible = true
            }
        )
    }

    if (isPlaylistSheetVisible) {
        AddToPlaylistSheet(
            playlists = playlists,
            onDismiss = { isPlaylistSheetVisible = false },
            onPlaylistSelected = { playlistId ->
                onAddToPlaylist(playlistId, track)
                isPlaylistSheetVisible = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerOptionsSheet(
    track: Track,
    downloadState: DownloadState,
    onDismiss: () -> Unit,
    onDownloadClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            // Track Info Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = track.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            SheetMenuItem(
                icon = Icons.Rounded.PlaylistAdd,
                label = "Add to Playlist",
                onClick = onAddToPlaylistClick
            )
            
            SheetMenuItem(
                icon = when (downloadState) {
                    is DownloadState.Idle -> Icons.Rounded.CloudDownload
                    is DownloadState.Downloading -> Icons.Rounded.CloudDownload
                    is DownloadState.Downloaded -> Icons.Rounded.CheckCircle
                },
                label = when (downloadState) {
                    is DownloadState.Idle -> "Download"
                    is DownloadState.Downloading -> {
                        if (downloadState.progress < 0f) "Downloading..."
                        else {
                            val pct = (downloadState.progress * 100).toInt()
                            "Downloading... $pct%"
                        }
                    }
                    is DownloadState.Downloaded -> "Downloaded"
                },
                onClick = onDownloadClick,
                trailing = {
                    if (downloadState is DownloadState.Downloading) {
                        val progress = if (downloadState.progress >= 0f) downloadState.progress else -1f
                        CircularProgressIndicator(
                            progress = { if (progress >= 0f) progress else 0f },
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            strokeWidth = 2.dp
                        )
                    }
                }
            )
            
            SheetMenuItem(
                icon = Icons.Rounded.Album,
                label = "Go to Album",
                onClick = { /* TODO */ }
            )
            
            SheetMenuItem(
                icon = Icons.Rounded.Person,
                label = "Go to Artist",
                onClick = { /* TODO */ }
            )
            
            SheetMenuItem(
                icon = Icons.Rounded.Info,
                label = "Song Info / Credits",
                onClick = { /* TODO */ }
            )
            
            SheetMenuItem(
                icon = Icons.Rounded.Share,
                label = "Share",
                onClick = { /* TODO */ }
            )
            
            SheetMenuItem(
                icon = Icons.Rounded.Timer,
                label = "Sleep Timer",
                onClick = { /* TODO */ }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    playlists: List<com.example.alphamusic.core.data.local.PlaylistEntity>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "Add to Playlist",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            SheetMenuItem(
                icon = Icons.Rounded.Add,
                label = "New Playlist",
                onClick = { /* TODO */ }
            )
            
            // Real Playlists
            if (playlists.isEmpty()) {
                Text(
                    "No playlists created yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                playlists.forEach { playlist ->
                    SheetMenuItem(
                        icon = Icons.Rounded.QueueMusic,
                        label = playlist.name,
                        onClick = {
                            onPlaylistSelected(playlist.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SheetMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { 
            Text(
                text = label, 
                color = MaterialTheme.colorScheme.onSurface
            ) 
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = trailing,
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
