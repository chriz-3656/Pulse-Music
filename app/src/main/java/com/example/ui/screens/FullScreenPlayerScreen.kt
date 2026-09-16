package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.RepeatMode
import com.example.domain.model.Song
import com.example.ui.components.AnalogVUMeter
import com.example.ui.components.LcdBadge
import com.example.ui.components.QualityBadge
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
import com.example.ui.theme.SkeuoPeakRed
import com.example.ui.theme.SkeuoPhosphorGreen
import com.example.ui.theme.SkeuoRecessedTray
import com.example.ui.theme.SkeuoTextPrimary
import com.example.ui.theme.SkeuoTextSecondary
import com.example.ui.theme.SkeuoTextTertiary
import com.example.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayerScreen(
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState = uiState.playerState
    val song = playerState.currentSong ?: return

    var isSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableFloatStateOf(0f) }

    val currentPosition = if (isSeeking) seekPositionMs.toLong() else playerState.currentPositionMs
    val totalDuration = if (playerState.totalDurationMs > 0L) playerState.totalDurationMs else (song.durationSec * 1000L)
    val sliderValue = if (totalDuration > 0L) (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f

    // Hi-Fi Hardware Deck Canvas
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SkeuoDeckDark)
            .statusBarsPadding()
            .testTag("full_screen_player_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Deck Header Plate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tactile Eject/Collapse Button
                SkeuoTactileButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("player_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = SkeuoChromeLight,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center Hi-Fi Model Plate
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SkeuoLedLamp(isLit = playerState.isPlaying, size = 6.dp, color = SkeuoAmberGlow)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PULSE AUDIO DECK • HI-FI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = SkeuoAmberGlow
                        )
                    }
                    Text(
                        text = song.album.ifBlank { "Direct Stream" },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = SkeuoTextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            if (song.albumId.isNotBlank()) {
                                onDismiss()
                                onAlbumClick(song.albumId)
                            }
                        }
                    )
                }

                QualityBadge(quality = playerState.audioQuality)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tactile Turntable / Vinyl Record Bay
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                SkeuoRecessedTray,
                                SkeuoDeckDark,
                                SkeuoKnobGrip,
                                SkeuoDeckDark
                            )
                        )
                    )
                    .border(
                        BorderStroke(
                            4.dp,
                            Brush.sweepGradient(
                                listOf(
                                    SkeuoChromeDark,
                                    SkeuoChromeMid,
                                    SkeuoChromeDark,
                                    SkeuoBevelHighlight,
                                    SkeuoChromeDark
                                )
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl Groove Rings
                Box(
                    modifier = Modifier
                        .size(236.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0x22FFFFFF), CircleShape)
                        .border(3.dp, Color(0x15000000), CircleShape)
                )

                // Center Album Label Disc
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(CircleShape)
                        .border(2.dp, SkeuoChromeMid, CircleShape)
                ) {
                    AsyncImage(
                        model = song.artworkUrl,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Turntable Center Spindle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(SkeuoChromeLight, SkeuoChromeDark)
                            )
                        )
                        .border(1.dp, SkeuoBevelHighlight, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Backlit LCD Display Bay (Metadata & Status)
            SkeuoBevelCard(
                modifier = Modifier.fillMaxWidth(),
                isRecessed = true,
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = SkeuoTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            color = SkeuoAmberGlow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                val artistIdToUse = if (song.artistId.isNotBlank()) song.artistId else song.artist
                                onDismiss()
                                onArtistClick(artistIdToUse)
                            }
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val streamSourceTag = when {
                            song.streamUrl.contains("soundcloud", ignoreCase = true) ||
                            song.streamUrl.contains("sndcdn", ignoreCase = true) -> "STREAM: SOUNDCLOUD"
                            song.streamUrl.contains("jiosaavn", ignoreCase = true) ||
                            song.streamUrl.contains("saavn", ignoreCase = true) -> "STREAM: JIOSAAVN"
                            song.streamUrl.contains("googlevideo", ignoreCase = true) -> "STREAM: YOUTUBE"
                            song.streamUrl.isNotBlank() -> "STREAM: CDN DIRECT"
                            else -> "STREAM: CONNECTING"
                        }
                        LcdBadge(
                            text = streamSourceTag,
                            textColor = SkeuoLcdCyan
                        )
                    }

                    // Analog VU Level Meter on LCD Bay
                    AnalogVUMeter(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        isPlaying = playerState.isPlaying
                    )

                    // Download & Favorite Tactile Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.downloadTrack() },
                            enabled = !uiState.downloadInProgress,
                            modifier = Modifier.size(34.dp).testTag("player_download_button")
                        ) {
                            if (uiState.downloadInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = SkeuoAmberGlow
                                )
                            } else {
                                Icon(
                                    imageVector = if (uiState.isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = if (uiState.isDownloaded) SkeuoAmberGlow else SkeuoTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.toggleFavorite() },
                            modifier = Modifier.size(34.dp).testTag("player_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (uiState.isFavorite) SkeuoAmberGlow else SkeuoTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Playback Error Banner (if error occurred during playback)
            if (!playerState.playbackError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x33FF3D00), RoundedCornerShape(18.dp))
                        .border(1.dp, SkeuoPeakRed.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Stream Error",
                            tint = SkeuoPeakRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = playerState.playbackError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = SkeuoPeakRed,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Tactile Fader & VFD Timecode Display
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = sliderValue,
                    onValueChange = { frac: Float ->
                        isSeeking = true
                        seekPositionMs = frac * totalDuration.toFloat()
                    },
                    onValueChangeFinished = {
                        isSeeking = false
                        viewModel.seekTo(seekPositionMs.toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = SkeuoAmberGlow,
                        activeTrackColor = SkeuoAmberGlow,
                        inactiveTrackColor = SkeuoRecessedTray
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("player_seek_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LcdBadge(
                        text = formatTimecode(currentPosition),
                        textColor = SkeuoLcdCyan
                    )
                    LcdBadge(
                        text = formatTimecode(totalDuration),
                        textColor = SkeuoTextSecondary
                    )
                }
            }

            // Chunky Hardware Transport Console (Controls)
            SkeuoBevelCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Toggle Switch
                    SkeuoTactileButton(
                        onClick = { viewModel.toggleShuffle() },
                        isPressedOrActive = playerState.isShuffle,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp).testTag("player_shuffle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playerState.isShuffle) SkeuoAmberGlow else SkeuoTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Previous Track Key
                    SkeuoTactileButton(
                        onClick = { viewModel.skipToPrevious() },
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp).testTag("player_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = SkeuoChromeLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Master Play/Pause Milled Chrome Knob Button
                    SkeuoTactileButton(
                        onClick = { viewModel.togglePlayPause() },
                        shape = CircleShape,
                        isPressedOrActive = playerState.isPlaying,
                        accentColor = SkeuoAmberGlow,
                        modifier = Modifier.size(62.dp).testTag("player_play_pause_button")
                    ) {
                        if (playerState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = SkeuoAmberGlow
                            )
                        } else {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = if (playerState.isPlaying) SkeuoAmberGlow else SkeuoChromeLight,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Next Track Key
                    SkeuoTactileButton(
                        onClick = { viewModel.skipToNext() },
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp).testTag("player_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = SkeuoChromeLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Repeat Mode Toggle
                    SkeuoTactileButton(
                        onClick = { viewModel.toggleRepeatMode() },
                        isPressedOrActive = playerState.repeatMode != RepeatMode.OFF,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp).testTag("player_repeat_button")
                    ) {
                        Icon(
                            imageVector = when (playerState.repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (playerState.repeatMode != RepeatMode.OFF) SkeuoAmberGlow else SkeuoTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Bottom Auxiliary Ribbon (Lyrics & Queue Drawer Trigger)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeuoTactileButton(
                    onClick = { viewModel.toggleLyrics() },
                    isPressedOrActive = uiState.showLyrics,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(32.dp).testTag("player_lyrics_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FormatQuote, contentDescription = null, tint = SkeuoAmberGlow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LYRICS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = SkeuoTextPrimary)
                    }
                }

                SkeuoTactileButton(
                    onClick = { viewModel.toggleQueue() },
                    isPressedOrActive = uiState.showQueue,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(32.dp).testTag("player_queue_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.QueueMusic, contentDescription = null, tint = SkeuoAmberGlow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("QUEUE (${playerState.queue.size})", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = SkeuoTextPrimary)
                    }
                }
            }
        }
    }

    // Queue Bottom Sheet
    if (uiState.showQueue) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleQueue() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SkeuoDeckDark,
            contentColor = SkeuoTextPrimary
        ) {
            QueueSheetContent(
                queue = playerState.queue,
                currentIndex = playerState.currentIndex,
                onSongSelect = { index -> viewModel.playQueueIndex(index) },
                onRemove = { index -> viewModel.removeFromQueue(index) },
                onClear = { viewModel.clearQueue() },
                onClose = { viewModel.toggleQueue() }
            )
        }
    }

    // Lyrics Bottom Sheet
    if (uiState.showLyrics) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleLyrics() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SkeuoDeckDark,
            contentColor = SkeuoTextPrimary
        ) {
            LyricsSheetContent(
                lyrics = uiState.lyrics,
                isLoading = uiState.lyricsLoading,
                songTitle = song.title,
                artist = song.artist,
                onClose = { viewModel.toggleLyrics() }
            )
        }
    }
}

@Composable
private fun QueueSheetContent(
    queue: List<Song>,
    currentIndex: Int,
    onSongSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("queue_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeuoLedLamp(isLit = true, color = SkeuoAmberGlow)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PLAYBACK QUEUE (${queue.size})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = SkeuoTextPrimary
                )
            }
            Row {
                if (queue.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Queue", tint = SkeuoTextSecondary)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SkeuoTextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "QUEUE IS EMPTY",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                    color = SkeuoTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(queue) { index, song ->
                    val isCurrent = index == currentIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCurrent) SkeuoCardSurface else Color.Transparent)
                            .clickable { onSongSelect(index) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = if (isCurrent) SkeuoAmberGlow else SkeuoTextTertiary,
                            modifier = Modifier.width(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isCurrent) SkeuoAmberGlow else SkeuoTextPrimary,
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
                        IconButton(
                            onClick = { onRemove(index) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = SkeuoTextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsSheetContent(
    lyrics: String?,
    isLoading: Boolean,
    songTitle: String,
    artist: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("lyrics_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LYRICS DISPLAY",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = SkeuoAmberGlow
                )
                Text(
                    text = "$songTitle • $artist",
                    style = MaterialTheme.typography.bodySmall,
                    color = SkeuoTextSecondary
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = SkeuoTextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SkeuoBevelCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            isRecessed = true
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = SkeuoAmberGlow)
                } else if (lyrics.isNullOrBlank()) {
                    Text(
                        text = "NO LYRICS AVAILABLE",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        color = SkeuoTextSecondary
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = lyrics,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 26.sp
                                ),
                                color = SkeuoTextPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimecode(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
