package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AudioQuality
import com.example.domain.model.MusicProvider
import com.example.domain.model.ProviderStatus
import com.example.ui.components.LcdBadge
import com.example.ui.components.SkeuoAppLogo
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
import com.example.ui.theme.SkeuoLcdBg
import com.example.ui.theme.SkeuoLcdCyan
import com.example.ui.theme.SkeuoPeakRed
import com.example.ui.theme.SkeuoPhosphorGreen
import com.example.ui.theme.SkeuoRecessedTray
import com.example.ui.theme.SkeuoTextPrimary
import com.example.ui.theme.SkeuoTextSecondary
import com.example.ui.theme.SkeuoTextTertiary
import com.example.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val openUrl: (String) -> Unit = remember(context) {
        { url: String ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (_: Exception) { }
        }
    }

    val launchUpiPayment: () -> Unit = remember(context) {
        {
            try {
                val upiUri = Uri.parse("upi://pay?pa=8281937229@axl&pn=CHRIS%20MON%20SAJI&mc=0000&mode=02&purpose=00")
                val intent = Intent(Intent.ACTION_VIEW, upiUri)
                context.startActivity(Intent.createChooser(intent, "Pay with UPI"))
            } catch (_: Exception) { }
        }
    }

    val cacheSizeFormatted = remember(uiState.cacheSizeBytes) {
        val bytes = uiState.cacheSizeBytes
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        String.format("%.2f MB", mb)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeuoLedLamp(isLit = true, size = 6.dp, color = SkeuoAmberGlow)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DECK CONFIGURATION",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = SkeuoTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Section: Music Engine & Providers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsSectionHeader(icon = Icons.Default.Language, title = "Music Engine & Providers")
                SkeuoTactileButton(
                    onClick = { if (!uiState.isCheckingProviders) viewModel.checkProviders() },
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(6.dp),
                    isPressedOrActive = uiState.isCheckingProviders
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Test Providers",
                            modifier = Modifier.size(13.dp),
                            tint = if (uiState.isCheckingProviders) SkeuoAmberGlow else SkeuoTextSecondary
                        )
                        Text(
                            text = if (uiState.isCheckingProviders) "TESTING..." else "TEST ALL",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = SkeuoTextSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            SkeuoBevelCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Select active streaming engine. In Auto mode, Pulse routes between high-bitrate sources for instant, fail-safe playback.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = SkeuoTextSecondary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    val providers = MusicProvider.values()
                    providers.forEachIndexed { index, provider ->
                        val isSelected = uiState.userSettings.provider == provider
                        val status = uiState.providerStatuses[provider]
                        val isOperational = status?.isOperational ?: true
                        val latency = status?.latencyMs ?: 0L

                        ProviderOptionRow(
                            provider = provider,
                            isSelected = isSelected,
                            isOperational = isOperational,
                            latencyMs = latency,
                            details = status?.details,
                            onSelect = { viewModel.setProvider(provider) }
                        )

                        if (index < providers.size - 1) {
                            Divider(
                                color = SkeuoChromeDark.copy(alpha = 0.4f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Audio Quality
        item {
            SettingsSectionHeader(icon = Icons.Default.HighQuality, title = "Streaming Quality")
            Spacer(modifier = Modifier.height(8.dp))

            SkeuoBevelCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    QualityOptionRow(
                        title = "High Quality (320 kbps)",
                        subtitle = "Studio master clarity • Optimal for Wi-Fi & Hi-Fi",
                        isSelected = uiState.userSettings.audioQuality == AudioQuality.HIGH,
                        onSelect = { viewModel.setQuality(AudioQuality.HIGH) }
                    )
                    Divider(color = SkeuoChromeDark.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 6.dp))
                    QualityOptionRow(
                        title = "Standard (160 kbps)",
                        subtitle = "Fast streaming & data conservation",
                        isSelected = uiState.userSettings.audioQuality == AudioQuality.STANDARD,
                        onSelect = { viewModel.setQuality(AudioQuality.STANDARD) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Playback & Crossfade
        item {
            SettingsSectionHeader(icon = Icons.Default.Tune, title = "Hardware Playback Controls")
            Spacer(modifier = Modifier.height(8.dp))

            SkeuoBevelCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Crossfade Duration",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SkeuoTextPrimary
                            )
                            Text(
                                text = "Seamless analog overlap transition",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = SkeuoTextSecondary
                            )
                        }
                        LcdBadge(
                            text = "${uiState.userSettings.crossfadeDurationSec} SEC",
                            textColor = SkeuoAmberGlow
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Slider(
                        value = uiState.userSettings.crossfadeDurationSec.toFloat(),
                        onValueChange = { viewModel.setCrossfadeDuration(it.toInt()) },
                        valueRange = 0f..8f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = SkeuoAmberGlow,
                            activeTrackColor = SkeuoAmberGlow,
                            inactiveTrackColor = SkeuoRecessedTray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("crossfade_slider")
                    )

                    Divider(color = SkeuoChromeDark.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Continuous Autoplay Stream",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SkeuoTextPrimary
                            )
                            Text(
                                text = "Automatically cues similar tracks when queue finishes",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = SkeuoTextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.userSettings.autoplayEnabled,
                            onCheckedChange = { viewModel.toggleAutoplay(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SkeuoDeckDark,
                                checkedTrackColor = SkeuoAmberGlow,
                                uncheckedTrackColor = SkeuoRecessedTray
                            ),
                            modifier = Modifier.testTag("autoplay_switch")
                        )
                    }

                    Divider(color = SkeuoChromeDark.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Skip Dropped Tracks",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SkeuoTextPrimary
                            )
                            Text(
                                text = "Advances to next item if stream buffer stalls",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = SkeuoTextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.userSettings.autoSkipFailedTracks,
                            onCheckedChange = { viewModel.toggleAutoSkip(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SkeuoDeckDark,
                                checkedTrackColor = SkeuoAmberGlow,
                                uncheckedTrackColor = SkeuoRecessedTray
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Storage & Cache Management
        item {
            SettingsSectionHeader(icon = Icons.Default.Storage, title = "Offline Audio Cache")
            Spacer(modifier = Modifier.height(8.dp))

            SkeuoBevelCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Deck Buffer Storage",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = SkeuoTextPrimary
                            )
                            Text(
                                text = "Cached audio for immediate local replay",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = SkeuoTextSecondary
                            )
                        }
                        LcdBadge(text = cacheSizeFormatted, textColor = SkeuoLcdCyan)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Max Buffer Limit",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = SkeuoTextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(500, 1000, 2000).forEach { limit ->
                                val selected = uiState.userSettings.cacheSizeLimitMb == limit
                                SkeuoTactileButton(
                                    onClick = { viewModel.setCacheLimit(limit) },
                                    isPressedOrActive = selected,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text(
                                        text = if (limit >= 1000) "${limit / 1000} GB" else "$limit MB",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 9.sp
                                        ),
                                        color = if (selected) SkeuoAmberGlow else SkeuoTextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SkeuoTactileButton(
                        onClick = { showClearConfirmDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("clear_cache_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = SkeuoPeakRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CLEAR BUFFER CACHE", color = SkeuoPeakRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Audio Streaming Diagnostics & Live Telemetry
        item {
            SettingsSectionHeader(icon = Icons.Default.Tune, title = "Audio Stream Diagnostics")
            Spacer(modifier = Modifier.height(8.dp))

            val pState = uiState.playerState
            val currentTrack = pState.currentSong
            val isPlaying = pState.isPlaying
            val isBuffering = pState.isBuffering
            val hasError = !pState.playbackError.isNullOrBlank()

            SkeuoBevelCard(
                modifier = Modifier.fillMaxWidth(),
                isRecessed = true,
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkeuoLedLamp(
                                isLit = true,
                                size = 8.dp,
                                color = when {
                                    hasError -> SkeuoPeakRed
                                    isPlaying -> SkeuoPhosphorGreen
                                    isBuffering -> SkeuoAmberGlow
                                    else -> SkeuoTextTertiary
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "STREAM ENGINE STATUS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = SkeuoTextSecondary
                            )
                        }

                        LcdBadge(
                            text = when {
                                hasError -> "STREAM ERROR"
                                isPlaying -> "STREAMING ACTIVE"
                                isBuffering -> "BUFFERING..."
                                currentTrack != null -> "READY / PAUSED"
                                else -> "IDLE"
                            },
                            textColor = when {
                                hasError -> SkeuoPeakRed
                                isPlaying -> SkeuoPhosphorGreen
                                isBuffering -> SkeuoAmberGlow
                                else -> SkeuoTextTertiary
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Color(0x1FFFFFFF), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Track & Source Telemetry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Active Track",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = SkeuoTextSecondary
                        )
                        Text(
                            text = currentTrack?.title ?: "None Selected",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (currentTrack != null) SkeuoTextPrimary else SkeuoTextTertiary,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Resolved Stream Source",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = SkeuoTextSecondary
                        )
                        val sourceLabel = when {
                            currentTrack == null -> "—"
                            currentTrack.streamUrl.contains("soundcloud", ignoreCase = true) ||
                            currentTrack.streamUrl.contains("sndcdn", ignoreCase = true) -> "SoundCloud CDN"
                            currentTrack.streamUrl.contains("jiosaavn", ignoreCase = true) ||
                            currentTrack.streamUrl.contains("saavn", ignoreCase = true) -> "JioSaavn CDN"
                            currentTrack.streamUrl.contains("googlevideo", ignoreCase = true) -> "YouTube Media Stream"
                            currentTrack.streamUrl.isNotBlank() -> "Direct Media Stream"
                            else -> "Awaiting Resolution"
                        }
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (sourceLabel != "—" && sourceLabel != "Awaiting Resolution") SkeuoLcdCyan else SkeuoAmberGlow
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Stream Target URL",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = SkeuoTextSecondary
                        )
                        Text(
                            text = if (currentTrack?.streamUrl.isNullOrBlank()) "Empty / None" else currentTrack!!.streamUrl.take(35) + "...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = if (!currentTrack?.streamUrl.isNullOrBlank()) SkeuoLcdCyan else SkeuoTextTertiary
                        )
                    }

                    // Playback Error Diagnostics Banner (if any)
                    if (hasError) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x33FF3D00), RoundedCornerShape(6.dp))
                                .border(1.dp, SkeuoPeakRed.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = SkeuoPeakRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DIAGNOSTIC ERROR LOG",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = SkeuoPeakRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = pState.playbackError ?: "Unknown streaming error",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = SkeuoTextPrimary
                                )
                            }
                        }
                    }

                    // Retry Action
                    if (currentTrack != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        SkeuoTactileButton(
                            onClick = { viewModel.retryPlayback() },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = SkeuoAmberGlow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RETRY / RECONNECT STREAM", color = SkeuoAmberGlow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: About & Developer Details
        item {
            SettingsSectionHeader(icon = Icons.Default.Info, title = "System & Developer Info")
            Spacer(modifier = Modifier.height(8.dp))

            SkeuoBevelCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkeuoAppLogo(size = 38.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Pulse Hi-Fi Music Deck",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = SkeuoTextPrimary
                                )
                                Text(
                                    text = "v${com.example.BuildConfig.VERSION_NAME} • Skeuomorphic Hi-Fi Edition",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = SkeuoAmberGlow
                                )
                            }
                        }

                        SkeuoLedLamp(isLit = true, size = 8.dp, color = SkeuoPhosphorGreen)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Ultra-fast streaming architecture with physical tactile controls, low-latency audio pipelines, zero animation overhead, and 320kbps high-fidelity reproduction.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = SkeuoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = SkeuoChromeDark.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Developer Card
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SkeuoAmberGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LEAD ARCHITECT & RESEARCHER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = SkeuoAmberGlow
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Chriz-3656",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = SkeuoTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Developer and security researcher with a strong focus on building real-world applications and consent-based telemetry tools. Work spans from front-end streaming platforms to ethical security frameworks.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                        color = SkeuoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Links
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeuoTactileButton(
                            onClick = { openUrl("https://github.com/chriz-3656") },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("github_link_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = SkeuoAmberGlow, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "GITHUB",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = SkeuoAmberGlow
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = SkeuoAmberGlow.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                            }
                        }

                        SkeuoTactileButton(
                            onClick = { openUrl("https://chriz-3656.github.io/") },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("portfolio_link_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = SkeuoLcdCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PORTFOLIO",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = SkeuoLcdCyan
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = SkeuoLcdCyan.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    SkeuoTactileButton(
                        onClick = launchUpiPayment,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("support_developer_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Favorite, 
                                contentDescription = null, 
                                tint = Color(0xFFFF5252), 
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SUPPORT DEVELOPER (UPI)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color(0xFFFF5252)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Explicit About Button as requested
                    SkeuoTactileButton(
                        onClick = { showAboutDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("about_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = SkeuoTextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ABOUT DECK & DEVELOPER",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = SkeuoTextPrimary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(96.dp))
        }
    }

    // About & Developer Modal Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = SkeuoDeckElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeuoAppLogo(size = 32.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "PULSE HI-FI DECK",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = SkeuoTextPrimary
                        )
                        Text(
                            text = "v${com.example.BuildConfig.VERSION_NAME} • Skeuomorphic Edition",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = SkeuoAmberGlow
                        )
                    }
                }
            },
            text = {
                Column {
                    Text(
                        text = "High-fidelity, hardware-modeled music client designed for instant tactile playback, zero UI rendering lag, and offline caching.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = SkeuoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = SkeuoChromeDark.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SkeuoPhosphorGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DEVELOPER & SECURITY RESEARCHER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = SkeuoPhosphorGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Chriz-3656",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = SkeuoTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "I'm Chriz-3656, a developer and security researcher with a strong focus on building real-world applications and consent-based telemetry tools. My work spans from front-end streaming platforms to ethical security frameworks.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                        color = SkeuoTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeuoTactileButton(
                            onClick = { openUrl("https://github.com/chriz-3656") },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = SkeuoAmberGlow, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "GitHub",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SkeuoAmberGlow
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = SkeuoAmberGlow.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                            }
                        }

                        SkeuoTactileButton(
                            onClick = { openUrl("https://chriz-3656.github.io/") },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = SkeuoLcdCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Portfolio",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SkeuoLcdCyan
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = SkeuoLcdCyan.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                SkeuoTactileButton(
                    onClick = { showAboutDialog = false },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "DISMISS",
                        fontWeight = FontWeight.Bold,
                        color = SkeuoAmberGlow,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }
        )
    }

    // Confirmation Dialog for Clearing Cache
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = SkeuoDeckElevated,
            title = {
                Text(
                    "PURGE AUDIO CACHE?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = SkeuoPeakRed
                )
            },
            text = { Text("This will delete temporary streaming buffers. Downloaded tracks remain intact.", color = SkeuoTextSecondary) },
            confirmButton = {
                SkeuoTactileButton(
                    onClick = {
                        viewModel.clearCache()
                        showClearConfirmDialog = false
                    },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("PURGE", fontWeight = FontWeight.Bold, color = SkeuoPeakRed, modifier = Modifier.padding(horizontal = 12.dp))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("CANCEL", color = SkeuoTextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SkeuoAmberGlow,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ),
            color = SkeuoTextPrimary
        )
    }
}

@Composable
fun QualityOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = SkeuoAmberGlow, unselectedColor = SkeuoChromeDark)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = SkeuoTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = SkeuoTextSecondary
            )
        }
    }
}

@Composable
fun ProviderOptionRow(
    provider: MusicProvider,
    isSelected: Boolean,
    isOperational: Boolean,
    latencyMs: Long,
    details: String?,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) SkeuoAmberGlow.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onSelect)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = SkeuoAmberGlow,
                unselectedColor = SkeuoChromeDark
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) SkeuoAmberGlow else SkeuoTextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                SkeuoLedLamp(
                    isLit = isOperational,
                    size = 6.dp,
                    color = if (isOperational) SkeuoPhosphorGreen else SkeuoPeakRed
                )
                if (latencyMs > 0L) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${latencyMs}ms",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                        color = if (latencyMs < 500) SkeuoPhosphorGreen else SkeuoAmberGlow
                    )
                }
            }
            Text(
                text = details ?: provider.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = SkeuoTextSecondary
            )
        }
        if (isSelected) {
            Spacer(modifier = Modifier.width(4.dp))
            LcdBadge(
                text = "ACTIVE",
                textColor = SkeuoAmberGlow
            )
        }
    }
}
