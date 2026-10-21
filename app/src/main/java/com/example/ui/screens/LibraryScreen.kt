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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.DownloadStatus
import com.example.domain.model.DownloadTask
import com.example.domain.model.PlayerState
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LcdBadge
import com.example.ui.components.SkeuoBevelCard
import com.example.ui.components.SkeuoLedLamp
import com.example.ui.components.SkeuoTactileButton
import com.example.ui.components.SongItemRow
import com.example.ui.viewmodel.LibraryTab
import com.example.ui.viewmodel.LibraryViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    playerState: PlayerState,
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (String) -> Unit = {},
    onAddToPlaylist: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var newPlaylistTitle by remember { mutableStateOf("") }
    var showSpotifyDialog by remember { mutableStateOf(false) }
    var spotifyUrl by remember { mutableStateOf("") }
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
                    SkeuoLedLamp(isLit = true, size = 6.dp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AUDIO ARCHIVE",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
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
                            Icon(Icons.Default.Add, contentDescription = "New Playlist", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("NEW CRATE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = MaterialTheme.colorScheme.primary)
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
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tab Content
            when (uiState.selectedTab) {
                LibraryTab.PLAYLISTS -> {
                    PlaylistsTabContent(
                        onImportSpotifyClick = { showSpotifyDialog = true },
                        playlists = uiState.playlists,
                        onCreateClick = { viewModel.showCreatePlaylistDialog(true) },
                        onPlaylistClick = { playlist -> playlist.id?.let { onPlaylistClick(it) } },
                        onDeletePlaylist = { playlistId -> viewModel.deletePlaylist(playlistId) }
                    )
                }
                LibraryTab.FAVORITES -> {
                    FavoritesTabContent(
                        favoriteSongs = uiState.favoriteSongs,
                        playerState = playerState,
                        onPlayAll = { viewModel.playAll(uiState.favoriteSongs) },
                        onSongClick = { song -> viewModel.playSong(song, uiState.favoriteSongs) },
                        onFavoriteToggle = { song -> viewModel.toggleFavorite(song) },
                        onDownloadClick = { song -> viewModel.downloadSong(song) },
                        onPlayNext = { song -> viewModel.playNext(song) },
                        onAddToQueue = { song -> viewModel.addToQueue(song) },
                        onAddToPlaylist = { song -> onAddToPlaylist?.invoke(song) }
                    )
                }
                LibraryTab.DOWNLOADS -> {
                    DownloadsTabContent(
                        downloadedSongs = uiState.downloadedSongs,
                        downloadTasks = uiState.downloadTasks,
                        playerState = playerState,
                        onPlayAll = { viewModel.playAll(uiState.downloadedSongs) },
                        onSongClick = { song -> viewModel.playSong(song, uiState.downloadedSongs) },
                        onRemoveDownload = { songId -> viewModel.removeDownload(songId) },
                        onCancelDownloadTask = { songId -> viewModel.cancelDownloadTask(songId) },
                        onFavoriteToggle = { song -> viewModel.toggleFavorite(song) },
                        onPlayNext = { song -> viewModel.playNext(song) },
                        onAddToQueue = { song -> viewModel.addToQueue(song) },
                        onAddToPlaylist = { song -> onAddToPlaylist?.invoke(song) }
                    )
                }
            }
        }

        // Spotify Import Dialog
        if (showSpotifyDialog) {
            AlertDialog(
                onDismissRequest = { showSpotifyDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        "IMPORT SPOTIFY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF1DB954)
                    )
                },
                text = {
                    Column {
                        Text("Paste a public Spotify playlist link to import its tracks directly into Pulse Music.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = spotifyUrl,
                            onValueChange = { spotifyUrl = it },
                            label = { Text("Spotify URL", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1DB954),
                                focusedLabelColor = Color(0xFF1DB954),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = Color(0xFF1DB954),
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (spotifyUrl.isNotBlank()) {
                                viewModel.importSpotifyPlaylist(spotifyUrl)
                                showSpotifyDialog = false
                                spotifyUrl = ""
                            }
                        }
                    ) {
                        Text("IMPORT", color = Color(0xFF1DB954))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSpotifyDialog = false }) {
                        Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
        
        // Progress Dialog
        uiState.importProgress?.let { progress ->
            AlertDialog(
                onDismissRequest = { if (progress.isComplete) viewModel.dismissImportDialog() },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(if (progress.isComplete) "IMPORT COMPLETE" else "IMPORTING...", color = MaterialTheme.colorScheme.onBackground) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(progress.message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
                        if (!progress.isComplete) {
                            LinearProgressIndicator(
                                progress = { progress.progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF1DB954),
                                trackColor = MaterialTheme.colorScheme.background
                            )
                        } else if (progress.error != null) {
                            Text(progress.error, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    if (progress.isComplete) {
                        TextButton(onClick = { viewModel.dismissImportDialog() }) {
                            Text("OK", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }

        // Create Playlist Dialog
        if (uiState.showCreateDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.showCreatePlaylistDialog(false) },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        "NEW PLAYLIST CRATE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPlaylistTitle,
                            onValueChange = { newPlaylistTitle = it },
                            label = { Text("Playlist Title", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newPlaylistDesc,
                            onValueChange = { newPlaylistDesc = it },
                            label = { Text("Description (Optional)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
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
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.showCreatePlaylistDialog(false) }) {
                        Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    }
}

@Composable
fun PlaylistsTabContent(
    onImportSpotifyClick: () -> Unit,
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
                            .background(MaterialTheme.colorScheme.background)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
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
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Organize customized audio selections",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Import Spotify Card
        item {
            Spacer(modifier = Modifier.height(12.dp))
            SkeuoBevelCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onImportSpotifyClick() },
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
                            .background(MaterialTheme.colorScheme.background)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddLink,
                            contentDescription = "Import Spotify",
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Import from Spotify",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Sync external playlists",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            .background(MaterialTheme.colorScheme.background)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(16.dp))
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
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (playlist.description.isNotBlank()) playlist.description else "${playlist.trackCount} tracks",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Playlist",
                            tint = MaterialTheme.colorScheme.outline
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
    onSongClick: (Song) -> Unit,
    onFavoriteToggle: (Song) -> Unit = {},
    onDownloadClick: (Song) -> Unit = {},
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onAddToPlaylist: (Song) -> Unit = {}
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
                    LcdBadge(text = "${favoriteSongs.size} STARRED TRACKS", textColor = MaterialTheme.colorScheme.primary)
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
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PLAY ALL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            items(favoriteSongs, key = { it.id }) { song ->
                val isCurrent = playerState.currentSong?.id == song.id
                SongItemRow(
                    song = song,
                    isPlaying = playerState.isPlaying && isCurrent,
                    isCurrentTrack = isCurrent,
                    onClick = { onSongClick(song) },
                    onFavoriteToggle = { onFavoriteToggle(song) },
                    onDownloadClick = { onDownloadClick(song) },
                    onPlayNext = { onPlayNext(song) },
                    onAddToQueue = { onAddToQueue(song) },
                    onAddToPlaylist = { onAddToPlaylist(song) },
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
fun DownloadsTabContent(
    downloadedSongs: List<Song>,
    downloadTasks: List<DownloadTask> = emptyList(),
    playerState: PlayerState,
    onPlayAll: () -> Unit,
    onSongClick: (Song) -> Unit,
    onRemoveDownload: (String) -> Unit = {},
    onCancelDownloadTask: (String) -> Unit = {},
    onFavoriteToggle: (Song) -> Unit = {},
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onAddToPlaylist: (Song) -> Unit = {}
) {
    if (downloadedSongs.isEmpty() && downloadTasks.isEmpty()) {
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
            // Live In-Progress Downloads Section
            if (downloadTasks.isNotEmpty()) {
                val activeCollection = downloadTasks.firstOrNull { !it.sourceCollection.isNullOrBlank() }?.sourceCollection
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LcdBadge(
                                text = "${downloadTasks.size} DOWNLOADING NOW",
                                textColor = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (!activeCollection.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "⚡ DOWNLOADING $activeCollection",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                items(downloadTasks, key = { "downloading_${it.song.id}" }) { task ->
                    DownloadingSongRow(
                        task = task,
                        onCancel = { onCancelDownloadTask(task.song.id) },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // Completed Offline Songs Section
            if (downloadedSongs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LcdBadge(text = "${downloadedSongs.size} OFFLINE MASTER COPIES", textColor = MaterialTheme.colorScheme.tertiary)
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
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PLAY OFFLINE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
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
                        onPlayNext = { onPlayNext(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadingSongRow(
    task: DownloadTask,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = task.song
    val isDownloading = task.status == DownloadStatus.DOWNLOADING

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .border(
                1.dp,
                if (isDownloading) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AsyncImage(
                        model = song.artworkUrl,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Song Title & Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${song.artist} • ${if (isDownloading) "DOWNLOADING ${(task.progress * 100).toInt()}%" else "QUEUED IN LINE"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isDownloading) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isDownloading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Cancel Button
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Download",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

