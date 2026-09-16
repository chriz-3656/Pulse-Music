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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.ErrorView
import com.example.ui.components.LcdBadge
import com.example.ui.components.LoadingView
import com.example.ui.components.SkeuoBevelCard
import com.example.ui.components.SkeuoTactileButton
import com.example.ui.components.SongItemRow
import com.example.ui.theme.SkeuoAmberGlow
import com.example.ui.theme.SkeuoChromeDark
import com.example.ui.theme.SkeuoChromeLight
import com.example.ui.theme.SkeuoDeckDark
import com.example.ui.theme.SkeuoRecessedTray
import com.example.ui.theme.SkeuoTextPrimary
import com.example.ui.theme.SkeuoTextSecondary
import com.example.ui.viewmodel.PlaylistDetailViewModel

@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel,
    playerState: PlayerState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SkeuoDeckDark)
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
                                tint = SkeuoChromeLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        LcdBadge(text = "PLAYLIST CRATE", textColor = SkeuoAmberGlow)
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
                                .background(SkeuoRecessedTray)
                                .border(BorderStroke(2.dp, SkeuoChromeDark), RoundedCornerShape(20.dp))
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
                            color = SkeuoTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (playlist.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = playlist.description,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = SkeuoTextSecondary,
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
                            color = SkeuoAmberGlow
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        SkeuoTactileButton(
                            onClick = { viewModel.playPlaylist() },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("playlist_play_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = SkeuoAmberGlow,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PLAY CRATE", fontWeight = FontWeight.Bold, color = SkeuoAmberGlow, letterSpacing = 1.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Tracklist Items
                itemsIndexed(playlist.songs) { index, song ->
                    val isPlayingThis = playerState.currentSong?.id == song.id
                    SongItemRow(
                        song = song,
                        isPlaying = playerState.isPlaying && isPlayingThis,
                        isCurrentTrack = isPlayingThis,
                        onClick = { viewModel.playTrack(song) },
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
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
