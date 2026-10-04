package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.player.MusicPlayerController
import com.example.data.repository.JamSessionManager
import com.example.di.AppContainer
import com.example.domain.model.JamRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.domain.usecase.SearchMusicUseCase
import com.example.domain.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay


data class JamUiState(
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val activeRoom: JamRoom? = null
)


class JamViewModel(
    val jamSessionManager: JamSessionManager,
    private val searchMusicUseCase: SearchMusicUseCase,
    private val playerController: MusicPlayerController
) : ViewModel() {

    private var searchJob: Job? = null
    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    fun searchSongs(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            try {
                val results = searchMusicUseCase(query)
                _searchResults.value = results
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun removeFromQueue(index: Int) {
        val room = _uiState.value.activeRoom ?: return
        if (room.hostId != jamSessionManager.currentUserId) return
        viewModelScope.launch {
            jamSessionManager.removeFromQueue(room.roomId, index)
        }
    }

    fun voteSkip() {
        val roomCode = _uiState.value.activeRoom?.roomId ?: return
        viewModelScope.launch {
            jamSessionManager.voteSkip(roomCode)
        }
    }

    fun forceSkip() {
        val room = _uiState.value.activeRoom ?: return
        if (room.hostId != jamSessionManager.currentUserId) return
        viewModelScope.launch {
            playerController.skipToNext()
            jamSessionManager.resetSkipVotes(room.roomId)
        }
    }

    fun addToQueue(song: Song) {
        val roomCode = _uiState.value.activeRoom?.roomId ?: return
        viewModelScope.launch {
            jamSessionManager.addToQueue(roomCode, song)
        }
    }
    
    fun playSong(song: Song) {
        val queue = _uiState.value.activeRoom?.queue ?: listOf(song)
        playerController.playSong(song, queue)
    }
    
    fun kickParticipant(participantId: String) {
        val roomCode = _uiState.value.activeRoom?.roomId ?: return
        viewModelScope.launch {
            jamSessionManager.kickParticipant(roomCode, participantId)
        }
    }

    private val _uiState = MutableStateFlow(JamUiState())
    val uiState: StateFlow<JamUiState> = _uiState.asStateFlow()

    fun createRoom(hostName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
            try {
                val roomCode = jamSessionManager.createRoom(hostName)
                observeRoom(roomCode)
            } catch (e: Exception) {
                _uiState.update { it.copy(isConnecting = false, errorMessage = e.message) }
            }
        }
    }

    fun joinRoom(roomCode: String, guestName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
            try {
                jamSessionManager.joinRoom(roomCode, guestName)
                observeRoom(roomCode)
            } catch (e: Exception) {
                _uiState.update { it.copy(isConnecting = false, errorMessage = e.message) }
            }
        }
    }

    fun voteToSkip() {
        val room = _uiState.value.activeRoom ?: return
        viewModelScope.launch {
            try {
                jamSessionManager.voteSkip(room.roomId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun endRoom() {
        val currentCode = _uiState.value.activeRoom?.roomId
        if (currentCode != null) {
            viewModelScope.launch {
                try {
                    jamSessionManager.endRoom(currentCode)
                } catch (e: Exception) {
                    // Ignore
                }
                _uiState.update { it.copy(activeRoom = null) }
            }
        }
    }

    fun leaveRoom() {
        val currentCode = _uiState.value.activeRoom?.roomId
        if (currentCode != null) {
            viewModelScope.launch {
                try {
                    jamSessionManager.leaveRoom(currentCode)
                } catch (e: Exception) {
                    // Ignore
                }
                _uiState.update { it.copy(activeRoom = null) }
            }
        }
    }

    private fun observeRoom(roomCode: String) {
        viewModelScope.launch {
            jamSessionManager.observeRoom(roomCode).collectLatest { room ->
                val wasInRoom = _uiState.value.activeRoom != null
                _uiState.update { it.copy(activeRoom = room, isConnecting = false) }
                
                if (room != null) {
                    // Kick detection
                    if (wasInRoom && room.hostId != jamSessionManager.currentUserId && !room.participants.containsKey(jamSessionManager.currentUserId)) {
                        _uiState.update { it.copy(activeRoom = null, errorMessage = "You have been kicked from the session by the host.") }
                        playerController.setPlayWhenReady(false)
                        return@collectLatest
                    }
                
                    if (room.queue.isNotEmpty()) {
                        playerController.syncQueue(room.queue)
                        
                        // Check skip votes
                        val participantCount = room.participants.size.coerceAtLeast(1)
                        val skipThreshold = (participantCount / 2.0).let { java.lang.Math.ceil(it).toInt() }.coerceAtLeast(1)
                        if (room.skipVotes.size >= skipThreshold && room.hostId == jamSessionManager.currentUserId) {
                            forceSkip()
                        }
                    }
                    
                    // Participant Live Sync Receiver
                    if (room.hostId != jamSessionManager.currentUserId) {
                        val state = playerController.playerState.value
                        val playback = room.playback
                        if (playback.currentSong != null) {
                            if (state.currentSong?.id != playback.currentSong.id) {
                                playerController.playSong(playback.currentSong, room.queue)
                            }
                            
                            playerController.setPlayWhenReady(playback.isPlaying)
                            
                            if (playback.isPlaying) {
                                val expectedPos = playback.positionMs + (System.currentTimeMillis() - playback.updatedAt)
                                // Add small delay buffer
                                val syncPos = expectedPos - 200
                                if (java.lang.Math.abs(state.currentPositionMs - syncPos) > 3000) {
                                    playerController.seekTo(syncPos.coerceAtLeast(0))
                                }
                            } else {
                                if (java.lang.Math.abs(state.currentPositionMs - playback.positionMs) > 3000) {
                                    playerController.seekTo(playback.positionMs)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Host Broadcaster
        viewModelScope.launch {
            var lastSyncedPosition = 0L
            var lastSyncedTime = 0L
            
            playerController.playerState.collectLatest { state ->
                val room = _uiState.value.activeRoom ?: return@collectLatest
                if (room.hostId == jamSessionManager.currentUserId) {
                
                    // 1. Autoplay suggestion bridge
                    if (state.queue.size > room.queue.size) {
                        val newSongs = state.queue.filter { s -> room.queue.none { it.id == s.id } }
                        for (song in newSongs) {
                            jamSessionManager.addToQueue(room.roomId, song)
                        }
                    }
                    
                    // 2. Playback State Sync
                    val lastPlayback = room.playback
                    val timeSinceUpdate = System.currentTimeMillis() - lastPlayback.updatedAt
                    val isStateChanged = lastPlayback.currentSong?.id != state.currentSong?.id || lastPlayback.isPlaying != state.isPlaying
                    
                    val expectedPos = if(lastPlayback.isPlaying) lastPlayback.positionMs + timeSinceUpdate else lastPlayback.positionMs
                    val isSeeked = java.lang.Math.abs(state.currentPositionMs - expectedPos) > 4000
                    
                    // Broadcast if state changed, user seeked, or every 5 seconds for heartbeat
                    if (isStateChanged || isSeeked || (System.currentTimeMillis() - lastSyncedTime > 5000)) {
                        val newPlayback = com.example.domain.model.JamPlaybackState(
                            currentSong = state.currentSong,
                            isPlaying = state.isPlaying,
                            positionMs = state.currentPositionMs,
                            updatedAt = System.currentTimeMillis()
                        )
                        jamSessionManager.updatePlaybackState(room.roomId, newPlayback)
                        lastSyncedPosition = state.currentPositionMs
                        lastSyncedTime = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(appContainer: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return JamViewModel(appContainer.jamSessionManager, appContainer.searchMusicUseCase, appContainer.playerController) as T
                }
            }
    }
}
