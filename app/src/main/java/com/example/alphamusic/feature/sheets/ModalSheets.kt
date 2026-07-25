package com.example.alphamusic.feature.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.alphamusic.core.domain.models.Track

import com.example.alphamusic.core.data.DownloadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsSheet(
    track: Track,
    downloadState: DownloadState,
    onDismissRequest: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onViewArtist: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 32.dp)
        ) {
            // Header
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
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = track.artistName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))

            // Options
            SheetOption(icon = Icons.Rounded.PlaylistPlay, text = "Play next", onClick = { onPlayNext(); onDismissRequest() })
            SheetOption(icon = Icons.Rounded.PlaylistAdd, text = "Add to playlist", onClick = { onAddToPlaylist(); onDismissRequest() })
            SheetOption(icon = Icons.Rounded.Person, text = "View artist", onClick = { onViewArtist(); onDismissRequest() })

            val downloadIcon = when (downloadState) {
                is DownloadState.Idle -> Icons.Rounded.Download
                is DownloadState.Downloading -> Icons.Rounded.CloudDownload
                is DownloadState.Downloaded -> Icons.Rounded.CheckCircle
            }
            val downloadText = when (downloadState) {
                is DownloadState.Idle -> "Download"
                is DownloadState.Downloading -> {
                    if (downloadState.progress < 0f) "Downloading..."
                    else {
                        val pct = (downloadState.progress * 100).toInt()
                        "Downloading... $pct%"
                    }
                }
                is DownloadState.Downloaded -> "Downloaded"
            }
            SheetOption(
                icon = downloadIcon,
                text = downloadText,
                onClick = { onDownload() },
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

            SheetOption(icon = Icons.Rounded.Share, text = "Share", onClick = { onShare(); onDismissRequest() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    onDismissRequest: () -> Unit,
    onTimerSet: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Sleep Timer",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            val options = listOf(15, 30, 45, 60)
            options.forEach { minutes ->
                SheetOption(
                    icon = Icons.Rounded.Timer,
                    text = "$minutes minutes",
                    onClick = { onTimerSet(minutes); onDismissRequest() }
                )
            }
            SheetOption(
                icon = Icons.Rounded.TimerOff,
                text = "Turn off timer",
                onClick = { onTimerSet(0); onDismissRequest() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrRenamePlaylistSheet(
    initialName: String = "",
    onDismissRequest: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }
    ) {
        Column(
            modifier = Modifier
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (initialName.isEmpty()) "New Playlist" else "Rename Playlist",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    cursorColor = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onSave(text); onDismissRequest() },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SheetOption(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}
