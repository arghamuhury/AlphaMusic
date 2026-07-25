package com.example.alphamusic.feature.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.alphamusic.core.ui.navigation.Screen
import com.example.alphamusic.core.data.DownloadState
import com.example.alphamusic.feature.home.HomeScreen
import com.example.alphamusic.feature.library.LibraryScreen
import com.example.alphamusic.feature.player.MiniPlayer
import com.example.alphamusic.feature.player.PlayerScreen
import com.example.alphamusic.feature.player.PlayerViewModel
import com.example.alphamusic.feature.search.SearchScreen
import com.example.alphamusic.feature.settings.SettingsScreen

@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val playbackPosition by playerViewModel.playbackPosition.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    val repeatMode by playerViewModel.repeatMode.collectAsState()

    val likedTracks by playerViewModel.likedTracks.collectAsState()
    val downloadedTracks by playerViewModel.downloadedTracks.collectAsState()
    val downloadStates by playerViewModel.downloadStates.collectAsState()
    val playlists by playerViewModel.playlists.collectAsState()

    var isPlayerExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                Column {
                    if (currentTrack != null) {
                        MiniPlayer(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            progress = if (duration > 0) playbackPosition.toFloat() / duration.toFloat() else 0f,
                            onPlayPauseClick = { playerViewModel.togglePlayPause() },
                            onNextClick = { playerViewModel.skipToNext() },
                            onPreviousClick = { playerViewModel.skipToPrevious() },
                            onClick = { isPlayerExpanded = true }
                        )
                    }
                    NavigationBar(
                        containerColor = Color.Black,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Home::class.qualifiedName } == true,
                            onClick = {
                                navController.navigate(Screen.Home) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Search::class.qualifiedName } == true,
                            onClick = {
                                navController.navigate(Screen.Search) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Library::class.qualifiedName } == true,
                            onClick = {
                                navController.navigate(Screen.Library) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                composable<Screen.Home> {
                    HomeScreen(
                        onTrackClick = { playerViewModel.playTrack(it) },
                        onPlayNextClick = { playerViewModel.playNext(it) },
                        onSettingsClick = { navController.navigate(Screen.Settings) }
                    )
                }
                composable<Screen.Search> {
                    SearchScreen(
                        onTrackClick = { playerViewModel.playTrack(it) },
                        onPlayNextClick = { playerViewModel.playNext(it) }
                    )
                }
                composable<Screen.Library> {
                    LibraryScreen(
                        onTrackClick = { playerViewModel.playTrack(it) },
                        onPlayNextClick = { playerViewModel.playNext(it) }
                    )
                }
                composable<Screen.Settings> {
                    SettingsScreen(onNavigateBack = { navController.navigateUp() })
                }
            }
        }

        // Full Screen Player Overlay
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerScreen(
                track = currentTrack,
                isPlaying = isPlaying,
                playbackPosition = playbackPosition,
                duration = duration,
                repeatMode = repeatMode,
                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                onSeekTo = { playerViewModel.seekTo(it) },
                onRepeatToggle = { playerViewModel.toggleRepeatMode() },
                onLikeToggle = { isLiked -> currentTrack?.let { playerViewModel.toggleLikeStatus(it, isLiked) } },
                onDownloadToggle = { isDownloaded ->
                    currentTrack?.let { track ->
                        if (isDownloaded) {
                            val currentState = downloadStates[track.id]
                            if (currentState !is DownloadState.Downloading) {
                                playerViewModel.downloadTrack(track)
                            }
                        } else {
                            playerViewModel.toggleDownloadStatus(track, false)
                        }
                    }
                },
                onAddToPlaylist = { playlistId, track -> playerViewModel.addTrackToPlaylist(playlistId, track) },
                isLiked = currentTrack?.let { track -> likedTracks.any { it.id == track.id } } ?: false,
                isDownloaded = currentTrack?.let { track -> downloadedTracks.any { it.id == track.id } } ?: false,
                downloadStates = downloadStates,
                playlists = playlists,
                onClose = { isPlayerExpanded = false }
            )
        }
    }
}
