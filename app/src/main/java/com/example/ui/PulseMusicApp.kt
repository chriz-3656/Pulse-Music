package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.PulseMusicApplication
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.SkeuoBevelCard
import com.example.ui.components.SkeuoLedLamp
import com.example.ui.components.SkeuoTactileButton
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.ArtistDetailScreen
import com.example.ui.screens.FullScreenPlayerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SkeuoAmberGlow
import com.example.ui.theme.SkeuoBevelHighlight
import com.example.ui.theme.SkeuoBevelShadow
import com.example.ui.theme.SkeuoChromeDark
import com.example.ui.theme.SkeuoChromeLight
import com.example.ui.theme.SkeuoDeckDark
import com.example.ui.theme.SkeuoDeckElevated
import com.example.ui.theme.SkeuoRecessedTray
import com.example.ui.theme.SkeuoTextPrimary
import com.example.ui.theme.SkeuoTextSecondary
import com.example.ui.theme.SkeuoTextTertiary
import com.example.ui.viewmodel.AlbumDetailViewModel
import com.example.ui.viewmodel.ArtistDetailViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.PlayerViewModel
import com.example.ui.viewmodel.PlaylistDetailViewModel
import com.example.ui.viewmodel.SearchViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.ViewModelFactory

enum class NavigationDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("DECK", Icons.Filled.Home, Icons.Outlined.Home),
    SEARCH("SEARCH", Icons.Filled.Search, Icons.Outlined.Search),
    LIBRARY("ARCHIVE", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    SETTINGS("SETUP", Icons.Filled.Settings, Icons.Outlined.Settings)
}

sealed class SubScreen {
    data class Artist(val artistId: String) : SubScreen()
    data class Album(val albumId: String) : SubScreen()
    data class Playlist(val playlistId: String) : SubScreen()
}

@Composable
fun PulseMusicApp() {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as PulseMusicApplication).appContainer
    val factory = remember { ViewModelFactory(appContainer) }

    val homeViewModel: HomeViewModel = viewModel(factory = factory)
    val searchViewModel: SearchViewModel = viewModel(factory = factory)
    val playerViewModel: PlayerViewModel = viewModel(factory = factory)
    val libraryViewModel: LibraryViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()

    var currentDestination by remember { mutableStateOf(NavigationDestination.HOME) }
    var isFullScreenPlayerOpen by remember { mutableStateOf(false) }

    val subScreenBackStack = remember { mutableStateListOf<SubScreen>() }

    BackHandler(enabled = subScreenBackStack.isNotEmpty() || isFullScreenPlayerOpen) {
        if (isFullScreenPlayerOpen) {
            isFullScreenPlayerOpen = false
        } else if (subScreenBackStack.isNotEmpty()) {
            subScreenBackStack.removeAt(subScreenBackStack.size - 1)
        }
    }

    // Request POST_NOTIFICATIONS permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkeuoDeckDark)
    ) {
        Scaffold(
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SkeuoDeckDark)
                ) {
                    // Mini player floating right above the navigation bar
                    MiniPlayerBar(
                        playerState = playerState,
                        onBarClick = { isFullScreenPlayerOpen = true },
                        onPlayPauseClick = { playerViewModel.togglePlayPause() },
                        onNextClick = { playerViewModel.skipToNext() }
                    )

                    // Skeuomorphic Selector Deck Bar
                    val navBarShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(navBarShape)
                            .background(SkeuoDeckElevated)
                            .border(BorderStroke(1.dp, SkeuoBevelHighlight), navBarShape)
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("bottom_navigation_bar")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavigationDestination.values().forEach { destination ->
                                val selected = currentDestination == destination && subScreenBackStack.isEmpty()
                                SkeuoTactileButton(
                                    onClick = {
                                        subScreenBackStack.clear()
                                        currentDestination = destination
                                    },
                                    isPressedOrActive = selected,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .padding(horizontal = 3.dp)
                                        .testTag("nav_item_${destination.name.lowercase()}")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                            contentDescription = destination.title,
                                            tint = if (selected) SkeuoAmberGlow else SkeuoTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = destination.title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                                                fontSize = 9.sp,
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = if (selected) SkeuoAmberGlow else SkeuoTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = SkeuoDeckDark,
            modifier = Modifier.statusBarsPadding()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val currentSubScreen = subScreenBackStack.lastOrNull()

                if (currentSubScreen != null) {
                    when (currentSubScreen) {
                        is SubScreen.Artist -> {
                            val artistVm = remember(currentSubScreen.artistId) {
                                ArtistDetailViewModel(currentSubScreen.artistId, appContainer)
                            }
                            ArtistDetailScreen(
                                viewModel = artistVm,
                                playerState = playerState,
                                onBackClick = { subScreenBackStack.removeAt(subScreenBackStack.size - 1) },
                                onAlbumClick = { albumId ->
                                    subScreenBackStack.add(SubScreen.Album(albumId))
                                }
                            )
                        }
                        is SubScreen.Album -> {
                            val albumVm = remember(currentSubScreen.albumId) {
                                AlbumDetailViewModel(currentSubScreen.albumId, appContainer)
                            }
                            AlbumDetailScreen(
                                viewModel = albumVm,
                                playerState = playerState,
                                onBackClick = { subScreenBackStack.removeAt(subScreenBackStack.size - 1) },
                                onArtistClick = { artistId ->
                                    subScreenBackStack.add(SubScreen.Artist(artistId))
                                }
                            )
                        }
                        is SubScreen.Playlist -> {
                            val playlistVm = remember(currentSubScreen.playlistId) {
                                PlaylistDetailViewModel(currentSubScreen.playlistId, appContainer)
                            }
                            PlaylistDetailScreen(
                                viewModel = playlistVm,
                                playerState = playerState,
                                onBackClick = { subScreenBackStack.removeAt(subScreenBackStack.size - 1) }
                            )
                        }
                    }
                } else {
                    when (currentDestination) {
                        NavigationDestination.HOME -> {
                            HomeScreen(
                                viewModel = homeViewModel,
                                playerState = playerState,
                                onSongClick = { song -> homeViewModel.playSong(song) },
                                onAlbumClick = { albumId ->
                                    subScreenBackStack.add(SubScreen.Album(albumId))
                                },
                                onPlaylistClick = { playlistId ->
                                    subScreenBackStack.add(SubScreen.Playlist(playlistId))
                                }
                            )
                        }
                        NavigationDestination.SEARCH -> {
                            SearchScreen(
                                viewModel = searchViewModel,
                                playerState = playerState,
                                onSongClick = { song -> searchViewModel.playSong(song) },
                                onArtistClick = { artistId ->
                                    subScreenBackStack.add(SubScreen.Artist(artistId))
                                },
                                onAlbumClick = { albumId ->
                                    subScreenBackStack.add(SubScreen.Album(albumId))
                                },
                                onPlaylistClick = { playlistId ->
                                    subScreenBackStack.add(SubScreen.Playlist(playlistId))
                                }
                            )
                        }
                        NavigationDestination.LIBRARY -> {
                            LibraryScreen(
                                viewModel = libraryViewModel,
                                playerState = playerState,
                                onSongClick = { song -> libraryViewModel.playSong(song, listOf(song)) },
                                onPlaylistClick = { playlistId ->
                                    subScreenBackStack.add(SubScreen.Playlist(playlistId))
                                }
                            )
                        }
                        NavigationDestination.SETTINGS -> {
                            SettingsScreen(viewModel = settingsViewModel)
                        }
                    }
                }
            }
        }

        // Full Screen Player Animated Overlay
        AnimatedVisibility(
            visible = isFullScreenPlayerOpen && playerState.currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            FullScreenPlayerScreen(
                viewModel = playerViewModel,
                onDismiss = { isFullScreenPlayerOpen = false },
                onArtistClick = { artistId ->
                    subScreenBackStack.add(SubScreen.Artist(artistId))
                },
                onAlbumClick = { albumId ->
                    subScreenBackStack.add(SubScreen.Album(albumId))
                }
            )
        }
    }
}
