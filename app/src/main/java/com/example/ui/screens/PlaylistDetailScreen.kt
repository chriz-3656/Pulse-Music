package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.PlayerState
import com.example.domain.model.Song
import com.example.ui.components.ErrorView
import com.example.ui.components.LcdBadge
import com.example.ui.components.LoadingView
import com.example.ui.components.SkeuoTactileButton
import com.example.ui.components.SongItemRow
import com.example.ui.viewmodel.PlaylistDetailViewModel

@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel,
    playerState: PlayerState,
    onBackClick: () -> Unit,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading) {
            LoadingView(message = "READING PLAYLIST CRATE...", modifier = Modifier.fillMaxSize())
        } else if (uiState.playlist != null) {
            val playlist = uiState.playlist!!
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Top App Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeuoTactileButton(
                            onClick = onBackClick,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("playlist_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        LcdBadge(text = "PLAYLIST CRATE", textColor = MaterialTheme.colorScheme.primary)
                    }
                }

                // Playlist Artwork & Header Information
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(20.dp))
                        ) {
                            AsyncImage(
                                model = playlist.artworkUrl,
                                contentDescription = playlist.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (playlist.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = playlist.description,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${playlist.trackCount} tracks in crate",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            SkeuoTactileButton(
                                onClick = { viewModel.playPlaylist() },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("playlist_play_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PLAY", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            SkeuoTactileButton(
                                onClick = {
                                    if (uiState.isAllDownloaded) {
                                        viewModel.removeAllDownloads()
                                    } else if (!uiState.isDownloadingAll) {
                                        viewModel.downloadAllSongs()
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                                    if (uiState.isDownloadingAll) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = uiState.downloadProgressText ?: "DOWNLOADING",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (uiState.isAllDownloaded) {
                                        Icon(
                                            imageVector = Icons.Filled.DownloadDone,
                                            contentDescription = "Downloaded",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("OFFLINE", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Download,
                                            contentDescription = "Download All",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("DOWNLOAD", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            SkeuoTactileButton(
                                onClick = { isSearchOpen = !isSearchOpen },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text(
                                    text = if (isSearchOpen) "CLOSE" else "+ ADD",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                        }

                        if (isSearchOpen) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    viewModel.searchSongs(it)
                                },
                                placeholder = { Text("Search songs to add...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            searchResults.forEach { searchHit ->
                                SongItemRow(
                                    song = searchHit,
                                    isPlaying = false,
                                    isCurrentTrack = false,
                                    onClick = {
                                        viewModel.addSongToPlaylist(searchHit)
                                        searchQuery = ""
                                        viewModel.searchSongs("")
                                        isSearchOpen = false
                                    },
                                    onPlayNext = { viewModel.playNext(searchHit) },
                                    onAddToQueue = { viewModel.addToQueue(searchHit) },
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Tracklist Items
                if (playlist.songs.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No songs in this playlist crate.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap \"+ ADD SONGS\" above to add tracks!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    itemsIndexed(playlist.songs) { index, song ->
                        val isPlayingThis = playerState.currentSong?.id == song.id
                        SongItemRow(
                            song = song,
                            isPlaying = playerState.isPlaying && isPlayingThis,
                            isCurrentTrack = isPlayingThis,
                            onClick = { viewModel.playTrack(song) },
                            onPlayNext = { viewModel.playNext(song) },
                            onAddToQueue = { viewModel.addToQueue(song) },
                            onFavoriteToggle = { viewModel.toggleFavorite(song) },
                            onDownloadClick = { viewModel.downloadSong(song) },
                            onRemoveFromPlaylist = { viewModel.removeSongFromPlaylist(song.id) },
                            onAddToPlaylist = onAddToPlaylist?.let { cb -> { cb(song) } },
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        } else {
            ErrorView(
                message = uiState.errorMessage ?: "Playlist details unavailable.",
                onRetry = onBackClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
