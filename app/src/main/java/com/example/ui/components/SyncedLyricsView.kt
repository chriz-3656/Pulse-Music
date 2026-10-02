package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalNeuColors

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
        return
    }

    val parsedLyrics = remember(lyrics) { parseLrc(lyrics) }
    
    if (parsedLyrics.isEmpty()) {
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

    val currentLine = if (activeIndex in parsedLyrics.indices) parsedLyrics[activeIndex].text else ""

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentLine,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(400)) { height -> height } + fadeIn(animationSpec = tween(400))) togetherWith
                        (slideOutVertically(animationSpec = tween(400)) { height -> -height } + fadeOut(animationSpec = tween(400)))
            },
            label = "lyrics_anim"
        ) { text ->
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = LocalNeuColors.current.accent
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}
