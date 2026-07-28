package com.example.alphamusic.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.stateIn
import com.example.alphamusic.core.data.local.PlaylistEntity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import coil.compose.AsyncImage
import com.example.alphamusic.core.domain.MusicRepository
import com.example.alphamusic.core.domain.models.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.alphamusic.core.ui.components.ScreenState
import com.example.alphamusic.core.ui.components.TrackListItem
import com.example.alphamusic.core.data.DownloadState
import com.example.alphamusic.feature.sheets.SongOptionsSheet
import com.example.alphamusic.feature.player.AddToPlaylistSheet
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {
    private val _trendingTracks = MutableStateFlow<List<Track>?>(null)
    val trendingTracks: StateFlow<List<Track>?> = _trendingTracks
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val playlists = repository.getAllPlaylists().stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getTrendingTracks()
                .onSuccess {
                    _trendingTracks.value = it
                    _isLoading.value = false
                }
                .onFailure {
                    it.printStackTrace()
                    _error.value = it.message ?: "Unknown error"
                    _isLoading.value = false
                }
        }
    }

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

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
    
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LargeTrackCard(
    track: Track,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp)
    ) {
        AsyncImage(
            model = track.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artistName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onTrackClick: (Track) -> Unit,
    onPlayNextClick: (Track) -> Unit,
    onSettingsClick: () -> Unit
) {
    val tracks by viewModel.trendingTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()

    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }
    var trackToAdd by remember { mutableStateOf<Track?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenState(
            isLoading = isLoading && tracks == null,
            error = error,
            data = tracks,
            onRetry = { viewModel.loadData() },
            loadingContent = { HomeShimmer() }
        ) { data ->
            LazyColumn(
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp, 
                    bottom = 24.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Discover",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Text(
                        text = "Quick Picks",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val chunked = data.chunked(4)
                        items(chunked) { columnTracks ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                columnTracks.forEach { track ->
                                    Box(modifier = Modifier.width(300.dp)) {
                                        TrackListItem(
                                            track = track, 
                                            onClick = { onTrackClick(track) },
                                            onLongClick = { selectedTrackForOptions = track },
                                            onMoreClick = { selectedTrackForOptions = track }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
                item {
                    Text(
                        text = "Your Mix",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                    )
                    val yourMix = remember(data) { data.shuffled().take(6) }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(yourMix) { track ->
                            LargeTrackCard(
                                track = track,
                                onClick = { onTrackClick(track) },
                                onLongClick = { selectedTrackForOptions = track }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    Text(
                        text = "Albums",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                    )
                    val albums = remember(data) { data.shuffled().take(6) }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(albums) { track ->
                            LargeTrackCard(
                                track = track,
                                onClick = { onTrackClick(track) },
                                onLongClick = { selectedTrackForOptions = track }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    Text(
                        text = "Trending",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                    )
                    val trending = remember(data) { data.shuffled().take(6) }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(trending) { track ->
                            LargeTrackCard(
                                track = track,
                                onClick = { onTrackClick(track) },
                                onLongClick = { selectedTrackForOptions = track }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    Text(
                        text = "New Releases",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                    )
                    val newReleases = remember(data) { data.shuffled().take(5) }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(newReleases) { track ->
                            LargeTrackCard(
                                track = track,
                                onClick = { onTrackClick(track) },
                                onLongClick = { selectedTrackForOptions = track }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
                }
        }
        
        selectedTrackForOptions?.let { track ->
            SongOptionsSheet(
                track = track,
                downloadState = downloadStates[track.id] ?: DownloadState.Idle,
                onDismissRequest = { selectedTrackForOptions = null },
                onPlayNext = {
                    onPlayNextClick(track)
                    selectedTrackForOptions = null
                },
                onAddToPlaylist = {
                    trackToAdd = track
                    selectedTrackForOptions = null
                },
                onViewArtist = { /* TODO */ },
                onShare = { /* TODO */ },
                onDownload = {
                    viewModel.downloadTrack(track)
                }
            )
        }

        val selectedDownloadState = selectedTrackForOptions?.let { downloadStates[it.id] }
        LaunchedEffect(selectedDownloadState) {
            if (selectedDownloadState is DownloadState.Downloaded) {
                selectedTrackForOptions = null
            }
        }

        trackToAdd?.let { trackToSave ->
            AddToPlaylistSheet(
                playlists = playlists,
                onDismiss = { trackToAdd = null },
                onPlaylistSelected = { playlistId ->
                    viewModel.addTrackToPlaylist(playlistId, trackToSave)
                    trackToAdd = null
                },
                onCreateNewPlaylist = { name ->
                    viewModel.createPlaylist(name)
                    trackToAdd = null
                }
            )
        }
    }
}
