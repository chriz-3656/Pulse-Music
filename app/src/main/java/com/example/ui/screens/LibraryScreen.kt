package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.PlayerState
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LcdBadge
import com.example.ui.components.SkeuoBevelCard
import com.example.ui.components.SkeuoLedLamp
import com.example.ui.components.SkeuoTactileButton
import com.example.ui.components.SongItemRow
import com.example.ui.theme.SkeuoAmberGlow
import com.example.ui.theme.SkeuoBevelHighlight
import com.example.ui.theme.SkeuoBevelShadow
import com.example.ui.theme.SkeuoCardSurface
import com.example.ui.theme.SkeuoChromeDark
import com.example.ui.theme.SkeuoChromeLight
import com.example.ui.theme.SkeuoChromeMid
import com.example.ui.theme.SkeuoDeckDark
import com.example.ui.theme.SkeuoDeckElevated
import com.example.ui.theme.SkeuoLcdBg
import com.example.ui.theme.SkeuoLcdCyan
import com.example.ui.theme.SkeuoRecessedTray
import com.example.ui.theme.SkeuoTextPrimary
import com.example.ui.theme.SkeuoTextSecondary
import com.example.ui.theme.SkeuoTextTertiary
import com.example.ui.viewmodel.LibraryTab
import com.example.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    playerState: PlayerState,
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var newPlaylistTitle by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Hardware Deck Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeuoLedLamp(isLit = true, size = 6.dp, color = SkeuoAmberGlow)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AUDIO ARCHIVE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = SkeuoTextPrimary
                    )
                }

                if (uiState.selectedTab == LibraryTab.PLAYLISTS) {
                    SkeuoTactileButton(
                        onClick = { viewModel.showCreatePlaylistDialog(true) },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("add_playlist_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Playlist", tint = SkeuoAmberGlow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("NEW CRATE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = SkeuoAmberGlow)
                        }
                    }
                }
            }

            // Tactile Tabs Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryTab.values().forEach { tab ->
                    val selected = uiState.selectedTab == tab
                    val count = when (tab) {
                        LibraryTab.PLAYLISTS -> uiState.playlists.size
                        LibraryTab.FAVORITES -> uiState.favoriteSongs.size
                        LibraryTab.DOWNLOADS -> uiState.downloadedSongs.size
                    }
                    val label = when (tab) {
                        LibraryTab.PLAYLISTS -> "CRATES ($count)"
                        LibraryTab.FAVORITES -> "FAVORITES ($count)"
                        LibraryTab.DOWNLOADS -> "OFFLINE ($count)"
                    }

                    SkeuoTactileButton(
                        onClick = { viewModel.setTab(tab) },
                        isPressedOrActive = selected,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .testTag("library_tab_${tab.name}")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (selected) SkeuoAmberGlow else SkeuoTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tab Content
            when (uiState.selectedTab) {
                LibraryTab.PLAYLISTS -> {
                    PlaylistsTabContent(
                        playlists = uiState.playlists,
                        onCreateClick = { viewModel.showCreatePlaylistDialog(true) },
                        onPlaylistClick = { playlist -> onPlaylistClick(playlist.id) },
                        onDeletePlaylist = { playlistId -> viewModel.deletePlaylist(playlistId) }
                    )
                }
                LibraryTab.FAVORITES -> {
                    FavoritesTabContent(
                        favoriteSongs = uiState.favoriteSongs,
                        playerState = playerState,
                        onPlayAll = { viewModel.playAll(uiState.favoriteSongs) },
                        onSongClick = { song -> viewModel.playSong(song, uiState.favoriteSongs) }
                    )
                }
                LibraryTab.DOWNLOADS -> {
                    DownloadsTabContent(
                        downloadedSongs = uiState.downloadedSongs,
                        playerState = playerState,
                        onPlayAll = { viewModel.playAll(uiState.downloadedSongs) },
                        onSongClick = { song -> viewModel.playSong(song, uiState.downloadedSongs) },
                        onRemoveDownload = { songId -> viewModel.removeDownload(songId) },
                        onFavoriteToggle = { song -> viewModel.toggleFavorite(song) }
                    )
                }
            }
        }

        // Create Playlist Dialog
        if (uiState.showCreateDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.showCreatePlaylistDialog(false) },
                containerColor = SkeuoDeckElevated,
                title = {
                    Text(
                        "NEW PLAYLIST CRATE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = SkeuoAmberGlow
                    )
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPlaylistTitle,
                            onValueChange = { newPlaylistTitle = it },
                            label = { Text("Playlist Title", color = SkeuoTextSecondary) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SkeuoTextPrimary,
                                unfocusedTextColor = SkeuoTextPrimary,
                                focusedBorderColor = SkeuoAmberGlow,
                                unfocusedBorderColor = SkeuoChromeDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newPlaylistDesc,
                            onValueChange = { newPlaylistDesc = it },
                            label = { Text("Description (Optional)", color = SkeuoTextSecondary) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SkeuoTextPrimary,
                                unfocusedTextColor = SkeuoTextPrimary,
                                focusedBorderColor = SkeuoAmberGlow,
                                unfocusedBorderColor = SkeuoChromeDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    SkeuoTactileButton(
                        onClick = {
                            viewModel.createPlaylist(newPlaylistTitle, newPlaylistDesc)
                            newPlaylistTitle = ""
                            newPlaylistDesc = ""
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            "CREATE",
                            fontWeight = FontWeight.Bold,
                            color = SkeuoAmberGlow,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showCreatePlaylistDialog(false) }) {
                        Text("CANCEL", color = SkeuoTextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    }
}

@Composable
fun PlaylistsTabContent(
    playlists: List<Playlist>,
    onCreateClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onDeletePlaylist: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Create Playlist Card
        item {
            SkeuoBevelCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCreateClick)
                    .testTag("create_new_playlist_card"),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SkeuoRecessedTray)
                            .border(BorderStroke(1.dp, SkeuoChromeDark), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = SkeuoAmberGlow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CREATE NEW CRATE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = SkeuoTextPrimary
                        )
                        Text(
                            text = "Organize customized audio selections",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = SkeuoTextSecondary
                        )
                    }
                }
            }
        }

        items(playlists) { playlist ->
            SkeuoBevelCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistClick(playlist) },
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SkeuoRecessedTray)
                            .border(BorderStroke(1.dp, SkeuoChromeDark), RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = playlist.artworkUrl,
                            contentDescription = playlist.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = SkeuoTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (playlist.description.isNotBlank()) playlist.description else "${playlist.trackCount} tracks",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = SkeuoAmberGlow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Playlist",
                            tint = SkeuoTextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesTabContent(
    favoriteSongs: List<Song>,
    playerState: PlayerState,
    onPlayAll: () -> Unit,
    onSongClick: (Song) -> Unit
) {
    if (favoriteSongs.isEmpty()) {
        EmptyStateView(
            title = "No Starred Tracks",
            message = "Tap the star/heart button on any track to pin to your favorite archive.",
            icon = Icons.Outlined.FavoriteBorder
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LcdBadge(text = "${favoriteSongs.size} STARRED TRACKS", textColor = SkeuoAmberGlow)
                    SkeuoTactileButton(
                        onClick = onPlayAll,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("play_all_favorites")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SkeuoAmberGlow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PLAY ALL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SkeuoAmberGlow)
                        }
                    }
                }
            }

            items(favoriteSongs) { song ->
                val isCurrent = playerState.currentSong?.id == song.id
                SongItemRow(
                    song = song,
                    isPlaying = playerState.isPlaying && isCurrent,
                    isCurrentTrack = isCurrent,
                    onClick = { onSongClick(song) },
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
fun DownloadsTabContent(
    downloadedSongs: List<Song>,
    playerState: PlayerState,
    onPlayAll: () -> Unit,
    onSongClick: (Song) -> Unit,
    onRemoveDownload: (String) -> Unit = {},
    onFavoriteToggle: (Song) -> Unit = {}
) {
    if (downloadedSongs.isEmpty()) {
        EmptyStateView(
            title = "Offline Archive Empty",
            message = "Save tracks to local memory for instant zero-latency playback without network.",
            icon = Icons.Default.Download
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LcdBadge(text = "${downloadedSongs.size} OFFLINE MASTER COPIES", textColor = SkeuoLcdCyan)
                    SkeuoTactileButton(
                        onClick = onPlayAll,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("play_all_downloads")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SkeuoAmberGlow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PLAY OFFLINE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SkeuoAmberGlow)
                        }
                    }
                }
            }

            items(downloadedSongs, key = { it.id }) { song ->
                val isCurrent = playerState.currentSong?.id == song.id
                SongItemRow(
                    song = song,
                    isPlaying = playerState.isPlaying && isCurrent,
                    isCurrentTrack = isCurrent,
                    onClick = { onSongClick(song) },
                    onDownloadClick = { onRemoveDownload(song.id) },
                    onFavoriteToggle = { onFavoriteToggle(song) },
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}
