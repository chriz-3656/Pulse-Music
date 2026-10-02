package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalNeuColors
import kotlinx.coroutines.delay

data class LyricLine(val timestampMs: Long, val text: String)

fun parseLrc(lyricsStr: String): List<LyricLine> {
    val lines = lyricsStr.lines()
    val parsedLines = mutableListOf<LyricLine>()
    val regex = Regex("^\\[(\\d{2,}):(\\d{2})(?:\\.(\\d{2,3}))?\\](.*)")
    
    for (line in lines) {
        val match = regex.find(line)
        if (match != null) {
            val mins = match.groupValues[1].toLongOrNull() ?: 0L
            val secs = match.groupValues[2].toLongOrNull() ?: 0L
            val msStr = match.groupValues[3]
            val ms = if (msStr.isNotBlank()) {
                if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
            } else 0L
            
            val text = match.groupValues[4].trim()
            val totalMs = (mins * 60 * 1000) + (secs * 1000) + ms
            parsedLines.add(LyricLine(totalMs, text))
        } else if (line.isNotBlank() && !line.startsWith("[")) {
            // For plain text mixed in
            if (parsedLines.isEmpty()) {
                parsedLines.add(LyricLine(0L, line))
            } else {
                val lastTime = parsedLines.last().timestampMs
                parsedLines.add(LyricLine(lastTime + 2000L, line))
            }
        }
    }
    return parsedLines.sortedBy { it.timestampMs }
}

@Composable
fun SyncedLyricsView(
    lyrics: String?,
    currentPositionMs: Long,
    modifier: Modifier = Modifier
) {
    if (lyrics.isNullOrBlank()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "NO LYRICS AVAILABLE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = LocalNeuColors.current.textSecondary.copy(alpha = 0.5f)
                )
            )
        }
        return
    }

    val parsedLyrics = remember(lyrics) { parseLrc(lyrics) }
    
    if (parsedLyrics.isEmpty()) {
        // Plain text fallback
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "LYRICS NOT SYNCED",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = LocalNeuColors.current.textSecondary.copy(alpha = 0.5f)
                )
            )
        }
        return
    }

    // Find current active line
    var activeIndex = -1
    for (i in parsedLyrics.indices) {
        if (currentPositionMs >= parsedLyrics[i].timestampMs) {
            activeIndex = i
        } else {
            break
        }
    }
    if (activeIndex == -1 && parsedLyrics.isNotEmpty()) activeIndex = 0

    val listState = rememberLazyListState()

    // Auto-scroll
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < parsedLyrics.size) {
            // Try to center the active item (offset by roughly half the height)
            val offset = if (activeIndex > 0) 1 else 0
            listState.animateScrollToItem(maxOf(0, activeIndex - offset))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(parsedLyrics) { index, line ->
            val isActive = index == activeIndex
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.3f,
                animationSpec = tween(300), label = ""
            )
            val fontSize by animateFloatAsState(
                targetValue = if (isActive) 18f else 14f,
                animationSpec = tween(300), label = ""
            )

            Text(
                text = line.text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontSize = fontSize.sp,
                    color = if (isActive) LocalNeuColors.current.accent else LocalNeuColors.current.textPrimary
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = if (isActive) 8.dp else 4.dp)
                    .alpha(alpha)
            )
        }
    }
}
