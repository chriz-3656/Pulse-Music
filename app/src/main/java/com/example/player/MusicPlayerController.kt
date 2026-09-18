package com.example.player

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.domain.model.AudioQuality
import com.example.domain.model.PlayerState
import com.example.domain.model.RepeatMode
import com.example.domain.model.Song
import com.example.service.MusicPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MusicPlayerController(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // Service connection / listener callback
    var onServiceCommand: ((ServiceAction) -> Unit)? = null

    // Autoplay providers: takes Song or songId -> returns recommended songs
    var autoplaySongProvider: (suspend (Song) -> List<Song>)? = null
    var autoplayProvider: (suspend (String) -> List<Song>)? = null

    private val prefs = context.getSharedPreferences("pulse_music_settings", Context.MODE_PRIVATE)
    var isAutoplayEnabled: Boolean
        get() = prefs.getBoolean("autoplay_enabled", true)
        set(value) {
            prefs.edit().putBoolean("autoplay_enabled", value).apply()
        }

    private suspend fun fetchSuggestionsForSong(song: Song): List<Song> {
        try {
            autoplaySongProvider?.let {
                val results = it.invoke(song)
                if (results.isNotEmpty()) return results
            }
            autoplayProvider?.let {
                val results = it.invoke(song.id)
                if (results.isNotEmpty()) return results
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    init {
        // Start foreground service
        startService()
    }

    private fun startService() {
        try {
            val intent = Intent(context, MusicPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Throwable) {
            // Handled gracefully in tests or restricted background execution environments
        }
    }

    fun playSong(song: Song, newQueue: List<Song>? = null) {
        val queue = newQueue ?: listOf(song)
        val index = queue.indexOfFirst { it.id == song.id }.let { if (it >= 0) it else 0 }
        
        _playerState.update {
            it.copy(
                currentSong = song,
                queue = queue,
                currentIndex = index,
                isPlaying = true,
                isBuffering = true,
                currentPositionMs = 0L,
                totalDurationMs = song.durationSec * 1000L,
                playbackError = null
            )
        }
        onServiceCommand?.invoke(ServiceAction.PlayTrack(song, queue, index))

        // If playing a single track (radio mode) or near queue end, proactively pre-fetch suggestions
        if (isAutoplayEnabled && (autoplaySongProvider != null || autoplayProvider != null)) {
            if (queue.size == 1 || index >= queue.size - 1) {
                scope.launch {
                    try {
                        val suggestions = fetchSuggestionsForSong(song)
                        if (suggestions.isNotEmpty()) {
                            val current = _playerState.value
                            if (current.currentSong?.id == song.id) {
                                val filtered = suggestions.filter { sug -> current.queue.none { it.id == sug.id } }
                                if (filtered.isNotEmpty()) {
                                    _playerState.update { it.copy(queue = current.queue + filtered) }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun playRadio(song: Song) {
        playSong(song, listOf(song))
    }

    fun addToQueue(song: Song) {
        val current = _playerState.value
        if (current.currentSong == null || current.queue.isEmpty()) {
            playSong(song, listOf(song))
            return
        }
        val updated = current.queue + song
        _playerState.update { it.copy(queue = updated) }
    }

    fun playNext(song: Song) {
        val current = _playerState.value
        if (current.currentSong == null || current.queue.isEmpty()) {
            playSong(song, listOf(song))
            return
        }
        val insertIndex = (current.currentIndex + 1).coerceAtMost(current.queue.size)
        val mutable = current.queue.toMutableList()
        // Remove if existing later in queue
        val existingLater = mutable.subList(insertIndex, mutable.size).indexOfFirst { it.id == song.id }
        if (existingLater >= 0) {
            mutable.removeAt(insertIndex + existingLater)
        }
        mutable.add(insertIndex, song)
        _playerState.update { it.copy(queue = mutable) }
    }

    fun playQueue(queue: List<Song>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        val validIndex = startIndex.coerceIn(0, queue.size - 1)
        val song = queue[validIndex]
        
        _playerState.update {
            it.copy(
                currentSong = song,
                queue = queue,
                currentIndex = validIndex,
                isPlaying = true,
                isBuffering = true,
                currentPositionMs = 0L,
                playbackError = null
            )
        }
        onServiceCommand?.invoke(ServiceAction.PlayTrack(song, queue, validIndex))
    }

    fun togglePlayPause() {
        val currentState = _playerState.value
        if (currentState.currentSong == null) {
            if (currentState.queue.isNotEmpty()) {
                playSong(currentState.queue[0], currentState.queue)
            }
            return
        }

        val newPlaying = !currentState.isPlaying
        _playerState.update { it.copy(isPlaying = newPlaying) }
        onServiceCommand?.invoke(if (newPlaying) ServiceAction.Resume else ServiceAction.Pause)
    }

    fun seekTo(positionMs: Long) {
        _playerState.update { it.copy(currentPositionMs = positionMs) }
        onServiceCommand?.invoke(ServiceAction.SeekTo(positionMs))
    }

    fun skipToNext() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        var nextIndex = state.currentIndex + 1
        if (nextIndex >= state.queue.size) {
            if (state.repeatMode == RepeatMode.ALL) {
                nextIndex = 0
            } else if (isAutoplayEnabled && (autoplaySongProvider != null || autoplayProvider != null) && state.currentSong != null) {
                // Autoplay: fetch suggestions and keep playing
                val currentSong = state.currentSong
                _playerState.update { it.copy(isPlaying = true, isBuffering = true) }
                scope.launch {
                    try {
                        val suggestions = fetchSuggestionsForSong(currentSong)
                        val current = _playerState.value
                        val filtered = suggestions.filter { sug -> current.queue.none { it.id == sug.id } }
                        if (filtered.isNotEmpty()) {
                            val newQueue = current.queue + filtered
                            val playIdx = current.queue.size
                            val nextTrack = newQueue[playIdx]
                            _playerState.update {
                                it.copy(
                                    currentSong = nextTrack,
                                    queue = newQueue,
                                    currentIndex = playIdx,
                                    currentPositionMs = 0L,
                                    totalDurationMs = nextTrack.durationSec * 1000L,
                                    isPlaying = true,
                                    isBuffering = true,
                                    playbackError = null
                                )
                            }
                            onServiceCommand?.invoke(ServiceAction.PlayTrack(nextTrack, newQueue, playIdx))
                        } else {
                            _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _playerState.update { it.copy(isPlaying = false, isBuffering = false) }
                    }
                }
                return
            } else {
                return
            }
        }

        val nextSong = state.queue[nextIndex]
        _playerState.update {
            it.copy(
                currentSong = nextSong,
                currentIndex = nextIndex,
                currentPositionMs = 0L,
                totalDurationMs = nextSong.durationSec * 1000L,
                isPlaying = true,
                isBuffering = true,
                playbackError = null
            )
        }
        onServiceCommand?.invoke(ServiceAction.PlayTrack(nextSong, state.queue, nextIndex))

        // Pre-fetch suggestions when approaching the end of the queue
        if (isAutoplayEnabled && (autoplaySongProvider != null || autoplayProvider != null) && nextIndex >= state.queue.size - 2) {
            scope.launch {
                try {
                    val suggestions = fetchSuggestionsForSong(nextSong)
                    val current = _playerState.value
                    val filtered = suggestions.filter { sug -> current.queue.none { it.id == sug.id } }
                    if (filtered.isNotEmpty()) {
                        _playerState.update { it.copy(queue = current.queue + filtered) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun skipToPrevious() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        if (state.currentPositionMs > 3000L) {
            seekTo(0L)
            return
        }

        var prevIndex = state.currentIndex - 1
        if (prevIndex < 0) {
            if (state.repeatMode == RepeatMode.ALL) {
                prevIndex = state.queue.size - 1
            } else {
                seekTo(0L)
                return
            }
        }

        val prevSong = state.queue[prevIndex]
        _playerState.update {
            it.copy(
                currentSong = prevSong,
                currentIndex = prevIndex,
                currentPositionMs = 0L,
                totalDurationMs = prevSong.durationSec * 1000L,
                isPlaying = true,
                isBuffering = true,
                playbackError = null
            )
        }
        onServiceCommand?.invoke(ServiceAction.PlayTrack(prevSong, state.queue, prevIndex))
    }

    fun toggleShuffle() {
        _playerState.update {
            val newShuffle = !it.isShuffle
            it.copy(isShuffle = newShuffle)
        }
        onServiceCommand?.invoke(ServiceAction.SetShuffle(_playerState.value.isShuffle))
    }

    fun toggleRepeatMode() {
        _playerState.update {
            val nextMode = when (it.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            it.copy(repeatMode = nextMode)
        }
        onServiceCommand?.invoke(ServiceAction.SetRepeat(_playerState.value.repeatMode))
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val state = _playerState.value
        if (fromIndex !in state.queue.indices || toIndex !in state.queue.indices) return

        val mutableList = state.queue.toMutableList()
        val item = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, item)

        val currentSongId = state.currentSong?.id
        val newCurrentIndex = if (currentSongId != null) {
            mutableList.indexOfFirst { it.id == currentSongId }.coerceAtLeast(0)
        } else {
            state.currentIndex
        }

        _playerState.update {
            it.copy(queue = mutableList, currentIndex = newCurrentIndex)
        }
    }

    fun removeFromQueue(index: Int) {
        val state = _playerState.value
        if (index !in state.queue.indices) return

        val mutableList = state.queue.toMutableList()
        val removed = mutableList.removeAt(index)

        if (state.currentSong?.id == removed.id) {
            if (mutableList.isNotEmpty()) {
                val nextIdx = index.coerceAtMost(mutableList.size - 1)
                val nextSong = mutableList[nextIdx]
                playSong(nextSong, mutableList)
            } else {
                _playerState.update {
                    it.copy(
                        currentSong = null,
                        queue = emptyList(),
                        currentIndex = -1,
                        isPlaying = false,
                        currentPositionMs = 0L,
                        totalDurationMs = 0L
                    )
                }
                onServiceCommand?.invoke(ServiceAction.Pause)
            }
        } else {
            val newIndex = if (state.currentSong != null) {
                mutableList.indexOfFirst { it.id == state.currentSong.id }
            } else {
                0
            }
            _playerState.update {
                it.copy(queue = mutableList, currentIndex = newIndex)
            }
        }
    }

    fun clearQueue() {
        val currentSong = _playerState.value.currentSong
        if (currentSong != null) {
            _playerState.update { it.copy(queue = listOf(currentSong), currentIndex = 0) }
        } else {
            _playerState.update { it.copy(queue = emptyList(), currentIndex = -1) }
        }
    }

    fun setAudioQuality(quality: AudioQuality) {
        _playerState.update { it.copy(audioQuality = quality) }
        val currentSong = _playerState.value.currentSong
        if (currentSong != null) {
            val currentPos = _playerState.value.currentPositionMs
            onServiceCommand?.invoke(ServiceAction.ReloadStreamWithQuality(quality, currentPos))
        }
    }

    fun setCrossfade(seconds: Int) {
        _playerState.update { it.copy(crossfadeSec = seconds) }
    }

    fun updateProgress(currentMs: Long, totalMs: Long, bufferedMs: Long) {
        _playerState.update {
            it.copy(
                currentPositionMs = currentMs,
                totalDurationMs = if (totalMs > 0) totalMs else it.totalDurationMs,
                bufferedPositionMs = bufferedMs
            )
        }
    }

    fun updatePlaybackState(isPlaying: Boolean, isBuffering: Boolean, error: String? = null) {
        _playerState.update {
            it.copy(
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                playbackError = error
            )
        }
    }

    fun setAudioFocusLoss(isLoss: Boolean) {
        _playerState.update { it.copy(isAudioFocusLoss = isLoss) }
    }

    fun updateCurrentSong(song: Song) {
        _playerState.update { current ->
            val updatedQueue = current.queue.map { if (it.id == song.id) song else it }
            current.copy(
                currentSong = if (current.currentSong?.id == song.id) song else current.currentSong,
                queue = updatedQueue
            )
        }
    }
}

sealed class ServiceAction {
    data class PlayTrack(val song: Song, val queue: List<Song>, val index: Int) : ServiceAction()
    object Resume : ServiceAction()
    object Pause : ServiceAction()
    data class SeekTo(val positionMs: Long) : ServiceAction()
    data class SetShuffle(val isShuffle: Boolean) : ServiceAction()
    data class SetRepeat(val mode: RepeatMode) : ServiceAction()
    data class ReloadStreamWithQuality(val quality: AudioQuality, val currentPos: Long) : ServiceAction()
}
