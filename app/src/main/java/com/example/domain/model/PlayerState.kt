package com.example.domain.model

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val crossfadeSec: Int = 2,
    val playbackError: String? = null,
    val isAudioFocusLoss: Boolean = false
) {
    val progressFraction: Float
        get() = if (totalDurationMs > 0) (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedCurrentPosition: String
        get() {
            val totalSec = currentPositionMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return String.format("%d:%02d", min, sec)
        }

    val formattedTotalDuration: String
        get() {
            val totalSec = if (totalDurationMs > 0) totalDurationMs / 1000 else (currentSong?.durationSec?.toLong() ?: 0L)
            val min = totalSec / 60
            val sec = totalSec % 60
            return String.format("%d:%02d", min, sec)
        }

    val hasNext: Boolean
        get() = queue.isNotEmpty() && (currentIndex < queue.size - 1 || repeatMode == RepeatMode.ALL)

    val hasPrevious: Boolean
        get() = queue.isNotEmpty() && (currentIndex > 0 || currentPositionMs > 3000 || repeatMode == RepeatMode.ALL)
}
