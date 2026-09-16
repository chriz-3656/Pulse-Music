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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.domain.model.Song
import com.example.ui.components.ErrorView
import com.example.ui.components.LcdBadge
import com.example.ui.components.LoadingView
import com.example.ui.components.SectionHeader
import com.example.ui.components.SkeuoBevelCard
import com.example.ui.components.SkeuoTactileButton
import com.example.ui.components.SongItemRow
import com.example.ui.theme.SkeuoAmberGlow
import com.example.ui.theme.SkeuoCardSurface
import com.example.ui.theme.SkeuoChromeDark
import com.example.ui.theme.SkeuoChromeLight
import com.example.ui.theme.SkeuoDeckDark
import com.example.ui.theme.SkeuoRecessedTray
import com.example.ui.theme.SkeuoTextPrimary
import com.example.ui.theme.SkeuoTextSecondary
import com.example.ui.theme.SkeuoTextTertiary
import com.example.ui.viewmodel.ArtistDetailViewModel

@Composable
fun ArtistDetailScreen(
    viewModel: ArtistDetailViewModel,
    playerState: PlayerState,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SkeuoDeckDark)
    ) {
        if (uiState.isLoading) {
            LoadingView(message = "LOADING ARTIST PROFILE...", modifier = Modifier.fillMaxSize())
        } else if (uiState.artist != null) {
            val artist = uiState.artist!!
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Header Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        AsyncImage(
                            model = artist.imageUrl.ifBlank { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500" },
                            contentDescription = artist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.3f),
                                            SkeuoDeckDark.copy(alpha = 0.7f),
                                            SkeuoDeckDark
                                        )
                                    )
                                )
                        )

                        // Top Bar with Back Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SkeuoTactileButton(
                                onClick = onBackClick,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("artist_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = SkeuoChromeLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Artist Name & Play Buttons
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            LcdBadge(text = "FEATURED ARTIST", textColor = SkeuoAmberGlow)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = SkeuoTextPrimary
                            )
                        }
                    }
                }

                // Action Controls
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SkeuoTactileButton(
                            onClick = { viewModel.playAll() },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("artist_play_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SkeuoAmberGlow, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PLAY TOP SONGS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SkeuoAmberGlow)
                            }
                        }

                        SkeuoTactileButton(
                            onClick = { viewModel.shuffleAll() },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, tint = SkeuoChromeLight, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SHUFFLE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SkeuoChromeLight)
                            }
                        }
                    }
                }

                // Albums Section
                if (uiState.albums.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Discography", subtitle = "Albums & EPs")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.albums) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onAlbumClick(album.id) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Top Songs Section
                if (uiState.topSongs.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Popular Tracks", subtitle = "Most streamed master tracks")
                    }
                    items(uiState.topSongs) { song ->
                        val isCurrent = playerState.currentSong?.id == song.id
                        SongItemRow(
                            song = song,
                            isPlaying = playerState.isPlaying && isCurrent,
                            isCurrentTrack = isCurrent,
                            onClick = { viewModel.playTrack(song) },
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        } else {
            ErrorView(
                message = uiState.errorMessage ?: "Artist details unavailable.",
                onRetry = onBackClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
