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
import androidx.compose.ui.draw.shadow
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
import com.example.domain.model.PlayerState
import com.example.domain.model.Song
import com.example.ui.components.AnalogVUMeter
import com.example.ui.components.ErrorView
import com.example.ui.components.LcdBadge
import com.example.ui.components.LoadingView
import com.example.ui.components.SkeuoBevelCard
import com.example.ui.components.SkeuoLedLamp
import com.example.ui.components.SkeuoTactileButton
import com.example.ui.theme.SkeuoAmberGlow
import com.example.ui.theme.SkeuoBevelHighlight
import com.example.ui.theme.SkeuoBevelShadow
import com.example.ui.theme.SkeuoCardSurface
import com.example.ui.theme.SkeuoChromeDark
import com.example.ui.theme.SkeuoChromeLight
import com.example.ui.theme.SkeuoChromeMid
import com.example.ui.theme.SkeuoDeckDark
import com.example.ui.theme.SkeuoDeckElevated
import com.example.ui.theme.SkeuoKnobGrip
import com.example.ui.theme.SkeuoLcdBg
import com.example.ui.theme.SkeuoLcdCyan
import com.example.ui.theme.SkeuoRecessedTray
import com.example.ui.theme.SkeuoTextPrimary
import com.example.ui.theme.SkeuoTextSecondary
import com.example.ui.theme.SkeuoTextTertiary
import com.example.ui.viewmodel.AlbumDetailViewModel

@Composable
fun AlbumDetailScreen(
    viewModel: AlbumDetailViewModel,
    playerState: PlayerState,
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SkeuoDeckDark)
    ) {
        if (uiState.isLoading) {
            LoadingView(message = "READING ALBUM DISK...", modifier = Modifier.fillMaxSize())
        } else if (uiState.album != null) {
            val album = uiState.album!!
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
                                .testTag("album_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = SkeuoChromeLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        LcdBadge(text = "STUDIO ALBUM MASTER", textColor = SkeuoAmberGlow)
                    }
                }

                // Vinyl Center Stage
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(SkeuoRecessedTray)
                                .border(BorderStroke(2.dp, SkeuoChromeDark), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = album.artworkUrl,
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SkeuoChromeMid)
                                    .border(1.dp, SkeuoBevelHighlight, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = SkeuoTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = album.artist,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = SkeuoAmberGlow,
                            modifier = Modifier
                                .clickable {
                                    val idToUse = if (album.artistId.isNotBlank()) album.artistId else album.artist
                                    onArtistClick(idToUse)
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = listOfNotNull(
                                if (album.year.isNotBlank()) album.year else null,
                                "${album.trackCount} tracks",
                                "HI-FI 320K"
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = SkeuoTextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SkeuoTactileButton(
                            onClick = { viewModel.playAlbum() },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("album_play_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = SkeuoAmberGlow,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PLAY FULL ALBUM", fontWeight = FontWeight.Bold, color = SkeuoAmberGlow, letterSpacing = 1.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Tracklist Items
                itemsIndexed(album.songs) { index, song ->
                    val isPlayingThis = playerState.currentSong?.id == song.id
                    AlbumTrackItem(
                        trackNumber = index + 1,
                        song = song,
                        isCurrentPlaying = isPlayingThis,
                        isPlaying = playerState.isPlaying,
                        onClick = { viewModel.playTrack(song) },
                        modifier = Modifier.testTag("album_track_$index")
                    )
                }
            }
        } else {
            ErrorView(
                message = uiState.errorMessage ?: "Album details unavailable.",
                onRetry = onBackClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AlbumTrackItem(
    trackNumber: Int,
    song: Song,
    isCurrentPlaying: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrentPlaying) SkeuoCardSurface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(26.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrentPlaying) {
                    AnalogVUMeter(isPlaying = isPlaying)
                } else {
                    Text(
                        text = "$trackNumber",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = SkeuoTextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isCurrentPlaying) SkeuoAmberGlow else SkeuoTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = SkeuoTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = song.formattedDuration,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = if (isCurrentPlaying) SkeuoAmberGlow else SkeuoTextTertiary
            )
        }
    }
}
