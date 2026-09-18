package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.AudioQuality
import com.example.domain.model.Song
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
import com.example.ui.theme.SkeuoPhosphorGreen
import com.example.ui.theme.SkeuoRecessedTray
import com.example.ui.theme.SkeuoTextPrimary
import com.example.ui.theme.SkeuoTextSecondary
import com.example.ui.theme.SkeuoTextTertiary

/**
 * Tactile Skeuomorphic Beveled Surface
 */
@Composable
fun SkeuoBevelCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    isRecessed: Boolean = false,
    content: @Composable () -> Unit
) {
    val backgroundBrush = if (isRecessed) {
        Brush.verticalGradient(
            colors = listOf(SkeuoRecessedTray, SkeuoDeckDark)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(SkeuoCardSurface, SkeuoDeckElevated)
        )
    }

    val topBorderColor = if (isRecessed) SkeuoBevelShadow else SkeuoBevelHighlight
    val bottomBorderColor = if (isRecessed) SkeuoBevelHighlight.copy(alpha = 0.1f) else SkeuoBevelShadow

    Box(
        modifier = modifier
            .shadow(if (isRecessed) 0.dp else 4.dp, shape)
            .clip(shape)
            .background(backgroundBrush)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(topBorderColor, bottomBorderColor)
                    )
                ),
                shape
            )
    ) {
        content()
    }
}

/**
 * Tactile Milled Hardware Button
 */
@Composable
fun SkeuoTactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPressedOrActive: Boolean = false,
    shape: Shape = RoundedCornerShape(20.dp),
    accentColor: Color = SkeuoAmberGlow,
    content: @Composable () -> Unit
) {
    val buttonBrush = if (isPressedOrActive) {
        Brush.verticalGradient(
            colors = listOf(SkeuoRecessedTray, SkeuoDeckDark)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(SkeuoChromeDark, SkeuoDeckElevated, SkeuoDeckDark)
        )
    }

    val borderBrush = if (isPressedOrActive) {
        Brush.verticalGradient(
            colors = listOf(accentColor.copy(alpha = 0.8f), SkeuoBevelShadow)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(SkeuoBevelHighlight, SkeuoBevelShadow)
        )
    }

    Box(
        modifier = modifier
            .shadow(if (isPressedOrActive) 0.dp else 3.dp, shape)
            .clip(shape)
            .background(buttonBrush)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Fast, Lightweight Analog Segmented Audio Meter
 */
@Composable
fun AnalogVUMeter(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val segmentColors = listOf(
            SkeuoPhosphorGreen,
            SkeuoPhosphorGreen,
            SkeuoPhosphorGreen,
            SkeuoAmberGlow,
            SkeuoPeakRed
        )

        segmentColors.forEachIndexed { index, color ->
            val isLit = isPlaying && index <= 3
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((8 + index * 2).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isLit) color else color.copy(alpha = 0.2f))
            )
        }
    }
}

/**
 * Skeuomorphic Glowing LED Indicator Lamp
 */
@Composable
fun SkeuoLedLamp(
    isLit: Boolean,
    color: Color = SkeuoAmberGlow,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = if (isLit) {
                        listOf(color, color.copy(alpha = 0.8f), SkeuoDeckDark)
                    } else {
                        listOf(color.copy(alpha = 0.2f), SkeuoDeckDark)
                    }
                )
            )
            .border(
                BorderStroke(0.75.dp, if (isLit) SkeuoBevelHighlight else SkeuoBevelShadow),
                CircleShape
            )
    )
}

/**
 * Backlit Hi-Fi LCD Display Badge
 */
@Composable
fun LcdBadge(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = SkeuoLcdCyan
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SkeuoLcdBg)
            .border(
                BorderStroke(1.dp, Brush.verticalGradient(listOf(SkeuoBevelShadow, SkeuoBevelHighlight.copy(alpha = 0.15f)))),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            ),
            color = textColor
        )
    }
}

@Composable
fun QualityBadge(
    quality: AudioQuality,
    modifier: Modifier = Modifier
) {
    LcdBadge(
        text = "HI-FI • ${quality.bitrate}",
        modifier = modifier,
        textColor = SkeuoAmberGlow
    )
}

/**
 * Skeuomorphic Track Item Row
 */
@Composable
fun SongItemRow(
    song: Song,
    isPlaying: Boolean,
    isCurrentTrack: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val itemShape = RoundedCornerShape(16.dp)
    val backgroundBrush = if (isCurrentTrack) {
        Brush.verticalGradient(
            colors = listOf(SkeuoCardSurface, SkeuoDeckElevated)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(itemShape)
            .background(backgroundBrush)
            .then(
                if (isCurrentTrack) {
                    Modifier.border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(SkeuoAmberGlow.copy(alpha = 0.6f), SkeuoBevelShadow)
                            )
                        ),
                        itemShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("song_item_${song.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tactile Vinyl / Cassette Framed Thumbnail
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SkeuoRecessedTray)
                    .border(BorderStroke(1.dp, SkeuoChromeDark), RoundedCornerShape(14.dp))
            ) {
                AsyncImage(
                    model = song.artworkUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (isCurrentTrack) {
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
                                contentDescription = "Current",
                                tint = SkeuoAmberGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = if (isCurrentTrack) SkeuoAmberGlow else SkeuoTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (song.isDownloaded) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = SkeuoAmberGlow,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "${song.artist} • ${song.album.ifBlank { "Single" }}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = SkeuoTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Duration in tactile LCD style
            Text(
                text = song.formattedDuration,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                ),
                color = if (isCurrentTrack) SkeuoAmberGlow else SkeuoTextTertiary,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            // Tactile options button
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = SkeuoTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier
                        .background(SkeuoCardSurface)
                        .border(BorderStroke(1.dp, SkeuoChromeDark), RoundedCornerShape(16.dp))
                ) {
                    if (onFavoriteToggle != null) {
                        DropdownMenuItem(
                            text = { Text(if (song.isFavorite) "Remove Favorite" else "Add Favorite", color = SkeuoTextPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = SkeuoAmberGlow
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onFavoriteToggle()
                            }
                        )
                    }
                    if (onDownloadClick != null) {
                        DropdownMenuItem(
                            text = { Text(if (song.isDownloaded) "Remove Offline Copy" else "Download Track", color = SkeuoTextPrimary) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (song.isDownloaded) Icons.Default.Delete else Icons.Default.Download,
                                    contentDescription = null,
                                    tint = SkeuoAmberGlow
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDownloadClick()
                            }
                        )
                    }
                    if (onPlayNext != null) {
                        DropdownMenuItem(
                            text = { Text("Play Next", color = SkeuoTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SkeuoAmberGlow) },
                            onClick = {
                                menuExpanded = false
                                onPlayNext()
                            }
                        )
                    }
                    if (onAddToQueue != null) {
                        DropdownMenuItem(
                            text = { Text("Add to Queue", color = SkeuoTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = SkeuoAmberGlow) },
                            onClick = {
                                menuExpanded = false
                                onAddToQueue()
                            }
                        )
                    }
                    if (onStartRadio != null) {
                        DropdownMenuItem(
                            text = { Text("Start Radio", color = SkeuoTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = SkeuoAmberGlow) },
                            onClick = {
                                menuExpanded = false
                                onStartRadio()
                            }
                        )
                    }
                    if (onAddToPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = SkeuoTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = SkeuoAmberGlow) },
                            onClick = {
                                menuExpanded = false
                                onAddToPlaylist()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongListItem(
    song: Song,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SongItemRow(
        song = song,
        isPlaying = isPlaying,
        isCurrentTrack = isCurrentTrack,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Skeuomorphic Section Header with Brushed Deck Accent
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeuoLedLamp(isLit = true, size = 6.dp, color = SkeuoAmberGlow)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = SkeuoTextPrimary
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = SkeuoTextSecondary,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                )
            }
        }

        if (actionText != null && onActionClick != null) {
            SkeuoTactileButton(
                onClick = onActionClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = actionText.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = SkeuoAmberGlow,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Lightweight Loading State
 */
@Composable
fun LoadingView(
    message: String = "Reading Audio Deck...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        SkeuoBevelCard(
            modifier = Modifier.padding(24.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = SkeuoAmberGlow,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                LcdBadge(text = message.uppercase(), textColor = SkeuoAmberGlow)
            }
        }
    }
}

/**
 * Lightweight Error State
 */
@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        SkeuoBevelCard(
            modifier = Modifier.padding(24.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = SkeuoPeakRed,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "DECK I/O ERROR",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = SkeuoPeakRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = SkeuoTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                SkeuoTactileButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = SkeuoAmberGlow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RETRY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = SkeuoAmberGlow)
                    }
                }
            }
        }
    }
}

/**
 * Empty State
 */
@Composable
fun EmptyStateView(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.MusicNote,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SkeuoRecessedTray)
                    .border(BorderStroke(1.dp, SkeuoChromeDark), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SkeuoAmberGlow.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = SkeuoTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = SkeuoTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Pulse Wave App Logo with vibrant electric yellow waveform on pitch black canvas
 */
@Composable
fun SkeuoAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF14151B),
                        Color(0xFF070709),
                        Color(0xFF000000)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.5.dp,
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFFFEA00),
                            Color(0xFFFFD600),
                            Color(0xFFFFAB00),
                            Color(0xFFFFD600),
                            Color(0xFFFFEA00)
                        )
                    )
                ),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.72f)) {
            val w = this.size.width
            val h = this.size.height
            val cy = h / 2f

            // Equalizer harmonic backdrop bars
            val barY1 = cy - h * 0.18f
            val barY2 = cy + h * 0.18f
            val barColor = Color(0x33FFD600)
            val barStroke = w * 0.045f

            drawLine(barColor, androidx.compose.ui.geometry.Offset(w * 0.28f, barY1), androidx.compose.ui.geometry.Offset(w * 0.28f, barY2), barStroke, StrokeCap.Round)
            drawLine(barColor, androidx.compose.ui.geometry.Offset(w * 0.40f, cy - h * 0.32f), androidx.compose.ui.geometry.Offset(w * 0.40f, cy + h * 0.32f), barStroke, StrokeCap.Round)
            drawLine(barColor, androidx.compose.ui.geometry.Offset(w * 0.60f, cy - h * 0.28f), androidx.compose.ui.geometry.Offset(w * 0.60f, cy + h * 0.28f), barStroke, StrokeCap.Round)
            drawLine(barColor, androidx.compose.ui.geometry.Offset(w * 0.72f, barY1), androidx.compose.ui.geometry.Offset(w * 0.72f, barY2), barStroke, StrokeCap.Round)

            // Dynamic Pulse Waveform Path
            val path = Path().apply {
                moveTo(w * 0.05f, cy)
                lineTo(w * 0.20f, cy)
                lineTo(w * 0.28f, cy - h * 0.14f)
                lineTo(w * 0.36f, cy + h * 0.18f)
                lineTo(w * 0.43f, cy - h * 0.10f)
                lineTo(w * 0.48f, cy)
                lineTo(w * 0.54f, cy - h * 0.42f) // Main sharp pulse spike
                lineTo(w * 0.60f, cy + h * 0.42f) // Sub-bass low plunge
                lineTo(w * 0.66f, cy - h * 0.24f) // Harmonic rebound
                lineTo(w * 0.72f, cy + h * 0.14f)
                lineTo(w * 0.78f, cy - h * 0.08f)
                lineTo(w * 0.84f, cy)
                lineTo(w * 0.95f, cy)
            }

            // Glow layer
            drawPath(
                path = path,
                color = Color(0x4DFFD600),
                style = Stroke(
                    width = w * 0.14f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Core Electric Yellow Waveform
            drawPath(
                path = path,
                color = Color(0xFFFFD600),
                style = Stroke(
                    width = w * 0.075f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // White-hot core beam
            drawPath(
                path = path,
                color = Color(0xEEFFFFFF),
                style = Stroke(
                    width = w * 0.028f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Peak spark nodes
            drawCircle(
                color = Color(0xFFFFEA00),
                radius = w * 0.05f,
                center = androidx.compose.ui.geometry.Offset(w * 0.54f, cy - h * 0.42f)
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.025f,
                center = androidx.compose.ui.geometry.Offset(w * 0.54f, cy - h * 0.42f)
            )
        }
    }
}

