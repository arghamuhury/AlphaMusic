package com.example.alphamusic.feature.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphamusic.core.data.local.SearchQueryDao
import com.example.alphamusic.core.data.local.SearchQueryEntity
import com.example.alphamusic.core.domain.MusicRepository
import com.example.alphamusic.core.domain.models.Track
import com.example.alphamusic.core.ui.components.ScreenState
import com.example.alphamusic.core.ui.components.TrackListItem
import com.example.alphamusic.core.data.DownloadState
import com.example.alphamusic.feature.search.SearchShimmer
import com.example.alphamusic.feature.sheets.SongOptionsSheet
import com.example.alphamusic.feature.player.AddToPlaylistSheet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val searchQueryDao: SearchQueryDao
) : ViewModel() {
    private val _searchResults = MutableStateFlow<List<Track>?>(emptyList())
    val searchResults: StateFlow<List<Track>?> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val recentSearches: StateFlow<List<String>> = searchQueryDao.getRecentSearches()
        .map { list -> list.map { it.query } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val likedTracks = repository.getLikedTracks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    val downloadedTracks = repository.getDownloadedTracks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    val playlists = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun toggleLikeStatus(track: Track, isLiked: Boolean) {
        viewModelScope.launch { repository.toggleLikeStatus(track, isLiked) }
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
        viewModelScope.launch { repository.addTrackToPlaylist(playlistId, track) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .filter { it.length >= 2 }
                .distinctUntilChanged()
                .collect { query ->
                    _isLoading.value = true
                    _error.value = null
                    
                    searchQueryDao.insertSearchQuery(
                        SearchQueryEntity(query = query, timestamp = System.currentTimeMillis())
                    )

                    repository.searchTracks(query)
                        .onSuccess {
                            _searchResults.value = it
                            _isLoading.value = false
                        }
                        .onFailure {
                            it.printStackTrace()
                            _error.value = it.message ?: "Search failed"
                            _isLoading.value = false
                        }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isLoading.value = false
            _error.value = null
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            searchQueryDao.clearAll()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onTrackClick: (Track) -> Unit,
    onPlayNextClick: (Track) -> Unit
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val downloadStates by viewModel.downloadStates.collectAsState()

    var query by remember { mutableStateOf("") }
    val tabs = listOf("Top Results", "Songs", "Artists", "Albums", "Playlists")
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    var selectedTrackForOptions by remember { mutableStateOf<Track?>(null) }
    var showAddToPlaylistForTrack by remember { mutableStateOf<Track?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Pill-shaped Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { 
                query = it
                viewModel.onSearchQueryChanged(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            placeholder = { Text("What do you want to listen to?") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { 
                        query = ""
                        viewModel.onSearchQueryChanged("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(percent = 50), // Pill shape
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        )

        if (query.isBlank()) {
            // Pre-search State
            if (recentSearches.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Searches",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Text("Clear", color = MaterialTheme.colorScheme.primary)
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    items(recentSearches) { recentQuery ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    query = recentQuery
                                    viewModel.onSearchQueryChanged(recentQuery)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = recentQuery,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Play what you love",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Post-search State
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                edgePadding = 20.dp,
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
                ScreenState(
                    isLoading = isLoading && searchResults.isNullOrEmpty(),
                    error = error,
                    data = searchResults,
                    onRetry = { viewModel.onSearchQueryChanged(query) },
                    loadingContent = { SearchShimmer() },
                    emptyContent = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No results found for '$query'",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                ) { tracks ->
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(tracks ?: emptyList(), key = { it.id }) { track ->
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
            }
        )
    }
}
