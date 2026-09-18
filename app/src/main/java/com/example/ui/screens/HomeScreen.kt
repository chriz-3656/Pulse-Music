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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.setValue
import com.example.util.AppUpdater
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.domain.model.Album
import com.example.domain.model.PlayerState
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.ui.components.AnalogVUMeter
import com.example.ui.components.ErrorView
import com.example.ui.components.LcdBadge
import com.example.ui.components.LoadingView
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkeuoAppLogo
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
import com.example.ui.theme.SkeuoPeakRed
import com.example.ui.theme.SkeuoRecessedTray
import com.example.ui.theme.SkeuoTextPrimary
import com.example.ui.theme.SkeuoTextSecondary
import com.example.ui.theme.SkeuoTextTertiary
import com.example.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    playerState: PlayerState,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<AppUpdater.UpdateInfo?>(null) }
    var activeDownloadId by remember { mutableStateOf<Long?>(null) }
    var downloadProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        val info = AppUpdater.checkForUpdates()
        if (info != null && info.isUpdateAvailable) {
            updateInfo = info
        }
    }

    LaunchedEffect(activeDownloadId) {
        activeDownloadId?.let { id ->
            val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            var isDownloading = true
            while (isDownloading) {
                val query = android.app.DownloadManager.Query().setFilterById(id)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS)
                    val bytesDownloadedIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    
                    if (statusIndex >= 0 && bytesDownloadedIndex >= 0 && bytesTotalIndex >= 0) {
                        val status = cursor.getInt(statusIndex)
                        val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                        val bytesTotal = cursor.getLong(bytesTotalIndex)
                        
                        if (bytesTotal > 0) {
                            downloadProgress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                        }
                        
                        if (status == android.app.DownloadManager.STATUS_SUCCESSFUL || status == android.app.DownloadManager.STATUS_FAILED) {
                            isDownloading = false
                            if (status == android.app.DownloadManager.STATUS_SUCCESSFUL) {
                                activeDownloadId = null
                                updateInfo = null
                            }
                        }
                    }
                }
                cursor?.close()
                kotlinx.coroutines.delay(100)
            }
        }
    }

    if (updateInfo != null && activeDownloadId == null) {
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("Update Available", color = SkeuoTextPrimary) },
            text = {
                Column {
                    Text("Version ${updateInfo!!.latestVersion} is now available.", color = SkeuoTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(updateInfo!!.releaseNotes, color = SkeuoTextTertiary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    activeDownloadId = AppUpdater.downloadAndInstall(context, updateInfo!!.downloadUrl, "PulseMusic-${updateInfo!!.latestVersion}.apk")
                }) {
                    Text("Download & Install", color = SkeuoAmberGlow)
                }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) { Text("Later", color = SkeuoTextSecondary) }
            },
            containerColor = SkeuoDeckDark
        )
    } else if (activeDownloadId != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Downloading Update", color = SkeuoTextPrimary) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = SkeuoAmberGlow,
                        trackColor = SkeuoRecessedTray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        color = SkeuoTextSecondary,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = { },
            containerColor = SkeuoDeckDark
        )
    }

    if (uiState.isLoading && uiState.trendingSongs.isEmpty()) {
        LoadingView(message = "Reading Audio Streams...", modifier = modifier.fillMaxSize())
        return
    }

    if (uiState.errorMessage != null && uiState.trendingSongs.isEmpty()) {
        ErrorView(
            message = uiState.errorMessage ?: "Failed to load music streams",
            onRetry = { viewModel.loadData() },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Skeuomorphic Hi-Fi Deck Console Banner
        item {
            HeroBanner(
                onPlayFeatured = {
                    if (uiState.trendingSongs.isNotEmpty()) {
                        viewModel.playSong(uiState.trendingSongs.first())
                    }
                }
            )
        }

        // Live Audio Stream Diagnostic Alert (if stream error occurred)
        if (!playerState.playbackError.isNullOrBlank()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(Color(0x33FF3D00), RoundedCornerShape(18.dp))
                        .border(1.dp, SkeuoPeakRed.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Stream Warning",
                            tint = SkeuoPeakRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STREAM PLAYBACK WARNING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = SkeuoPeakRed
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = playerState.playbackError ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                ),
                                color = SkeuoTextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Trending Carousel Section
        item {
            SectionHeader(
                title = "Trending Master Tracks",
                subtitle = "High fidelity chart toppers",
                actionText = "Refresh",
                onActionClick = { viewModel.loadData() }
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                items(uiState.trendingSongs) { song ->
                    TrendingSongCard(
                        song = song,
                        isCurrent = playerState.currentSong?.id == song.id,
                        isPlaying = playerState.isPlaying && playerState.currentSong?.id == song.id,
                        onClick = { viewModel.playSong(song) }
                    )
                }
            }
        }

        // Featured Albums Section
        if (uiState.featuredAlbums.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                SectionHeader(
                    title = "Featured Vinyl & Albums",
                    subtitle = "Studio master recordings"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    items(uiState.featuredAlbums) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album.id) }
                        )
                    }
                }
            }
        }

        // Curated Playlists Section
        if (uiState.featuredPlaylists.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                SectionHeader(
                    title = "Curated Mixtapes",
                    subtitle = "Thematic audio archives"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    items(uiState.featuredPlaylists) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.id) }
                        )
                    }
                }
            }
        }

        // Quick Picks Track List
        item {
            Spacer(modifier = Modifier.height(10.dp))
            SectionHeader(
                title = "Direct Stream Feeds",
                subtitle = "Lossless & 320kbps acoustics"
            )
        }

        items(uiState.trendingSongs) { song ->
            SongItemRow(
                song = song,
                isPlaying = playerState.isPlaying && playerState.currentSong?.id == song.id,
                isCurrentTrack = playerState.currentSong?.id == song.id,
                onClick = { viewModel.playSong(song) },
                onPlayNext = { viewModel.playNext(song) },
                onAddToQueue = { viewModel.addToQueue(song) },
                onStartRadio = { viewModel.playRadio(song) },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun HeroBanner(
    onPlayFeatured: () -> Unit,
    modifier: Modifier = Modifier
) {
    SkeuoBevelCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeuoAppLogo(size = 48.dp)

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeuoLedLamp(isLit = true, size = 6.dp, color = SkeuoAmberGlow)
                    Spacer(modifier = Modifier.width(6.dp))
                    LcdBadge(
                        text = "STUDIO MASTER • HI-FI 320K",
                        textColor = SkeuoAmberGlow
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "High Fidelity Streaming",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = SkeuoTextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Lossless acoustics & ultra-fast playback response",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = SkeuoTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Master Play Tactile Button
            SkeuoTactileButton(
                onClick = onPlayFeatured,
                shape = CircleShape,
                accentColor = SkeuoAmberGlow,
                modifier = Modifier
                    .size(52.dp)
                    .testTag("hero_play_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Master Stream",
                    tint = SkeuoAmberGlow,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun TrendingSongCard(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SkeuoBevelCard(
        modifier = modifier
            .width(136.dp)
            .clickable(onClick = onClick)
            .testTag("trending_card_${song.id}"),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Vinyl Framed Image Box
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SkeuoRecessedTray)
                    .border(BorderStroke(1.dp, SkeuoChromeDark), RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = song.artworkUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlaying) {
                            AnalogVUMeter(isPlaying = true)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = SkeuoAmberGlow,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = if (isCurrent) SkeuoAmberGlow else SkeuoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = SkeuoTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SkeuoBevelCard(
        modifier = modifier
            .width(136.dp)
            .clickable(onClick = onClick)
            .testTag("album_card_${album.id}"),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SkeuoRecessedTray)
                    .border(BorderStroke(1.dp, SkeuoChromeDark), RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = album.artworkUrl,
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = SkeuoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${album.artist} • ${album.year}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = SkeuoTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SkeuoBevelCard(
        modifier = modifier
            .width(152.dp)
            .clickable(onClick = onClick)
            .testTag("playlist_card_${playlist.id}"),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 136.dp, height = 98.dp)
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = SkeuoTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.trackCount} tracks",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = SkeuoAmberGlow
            )
        }
    }
}
