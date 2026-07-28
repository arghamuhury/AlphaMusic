package com.example.alphamusic.feature.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.alphamusic.core.domain.MusicRepository
import com.example.alphamusic.core.domain.models.Track
import com.example.alphamusic.core.ui.components.ScreenState
import com.example.alphamusic.core.ui.components.TrackListItem
import com.example.alphamusic.core.data.DownloadState
import com.example.alphamusic.feature.sheets.SongOptionsSheet
import com.example.alphamusic.feature.player.AddToPlaylistSheet
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
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {
    val likedTracks = repository.getLikedTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val downloadedTracks = repository.getDownloadedTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val playlists = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name)
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

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    private val playlistTracksCache = mutableMapOf<String, StateFlow<List<Track>>>()

    fun getTracksForPlaylist(playlistId: String): StateFlow<List<Track>> {
        return playlistTracksCache.getOrPut(playlistId) {
            repository.getTracksForPlaylist(playlistId)
                .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onTrackClick: (Track) -> Unit,
    onPlayNextClick: (Track) -> Unit
) {
    val likedTracks by viewModel.likedTracks.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()
    
    val tabs = listOf("Songs", "Playlists", "Downloads")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }
    var showAddToPlaylistForTrack by remember { mutableStateOf<Track?>(null) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedPlaylistId != null) {
        selectedPlaylistId = null
    }

    val selectedPlaylist = selectedPlaylistId?.let { id -> playlists.find { it.id == id } }
    if (selectedPlaylist != null) {
        PlaylistDetailView(
            playlist = selectedPlaylist,
            viewModel = viewModel,
            onTrackClick = onTrackClick,
            onPlayNextClick = onPlayNextClick,
            onBack = { selectedPlaylistId = null },
            onTrackLongClick = { selectedTrackForOptions = it },
            onTrackMoreClick = { selectedTrackForOptions = it }
        )
    } else {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Large App Bar equivalent
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            FloatingActionButton(
                onClick = { showCreatePlaylistDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Playlist")
            }
        }

        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            edgePadding = 24.dp,
            divider = {},
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = Color.Transparent
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                val selected = pagerState.currentPage == index
                Tab(
                    selected = selected,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(text = title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    // Songs Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Rounded.SortByAlpha, contentDescription = "Sort Alpha", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(Icons.Rounded.DateRange, contentDescription = "Sort Date", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(Icons.Rounded.AccessTime, contentDescription = "Sort Duration", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        ScreenState(
                            isLoading = false,
                            error = null,
                            data = likedTracks,
                            onRetry = {},
                            loadingContent = { },
                            emptyContent = { 
                                LibraryEmptyContent(message = "No liked songs yet.") 
                            }
                        ) { tracks ->
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(tracks, key = { it.id }) { track ->
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
                1 -> {
                    // Playlists Tab
                    if (playlists.isEmpty()) {
                        LibraryEmptyContent(message = "No playlists yet.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(playlists.size, key = { playlists[it].id }) { index ->
                                val playlist = playlists[index]
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPlaylistId = playlist.id }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Downloads Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        ScreenState(
                            isLoading = false,
                            error = null,
                            data = downloadedTracks,
                            onRetry = {},
                            loadingContent = { },
                            emptyContent = { 
                                LibraryEmptyContent(message = "No downloaded songs yet.") 
                            }
                        ) { tracks ->
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(tracks, key = { it.id }) { track ->
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
            }
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
                showAddToPlaylistForTrack = track
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

    showAddToPlaylistForTrack?.let { trackToSave ->
        AddToPlaylistSheet(
            playlists = playlists,
            onDismiss = { showAddToPlaylistForTrack = null },
            onPlaylistSelected = { playlistId ->
                viewModel.addTrackToPlaylist(playlistId, trackToSave)
                showAddToPlaylistForTrack = null
            },
            onCreateNewPlaylist = { name ->
                viewModel.createPlaylist(name)
                showAddToPlaylistForTrack = null
            }
        )
    }

    if (showCreatePlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.createPlaylist(playlistName)
                    showCreatePlaylistDialog = false 
                    playlistName = ""
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PlaylistDetailView(
    playlist: com.example.alphamusic.core.data.local.PlaylistEntity,
    viewModel: LibraryViewModel,
    onTrackClick: (Track) -> Unit,
    onPlayNextClick: (Track) -> Unit,
    onBack: () -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onTrackMoreClick: (Track) -> Unit
) {
    val tracks by viewModel.getTracksForPlaylist(playlist.id).collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        ScreenState(
            isLoading = false,
            error = null,
            data = tracks,
            onRetry = {},
            loadingContent = { },
            emptyContent = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No tracks in this playlist yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        ) { trackList ->
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(trackList, key = { it.id }) { track ->
                    TrackListItem(
                        track = track,
                        onClick = { onTrackClick(track) },
                        onLongClick = { onTrackLongClick(track) },
                        onMoreClick = { onTrackMoreClick(track) }
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryEmptyContent(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
