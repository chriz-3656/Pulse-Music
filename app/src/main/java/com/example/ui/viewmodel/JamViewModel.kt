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
                _uiState.update { it.copy(activeRoom = room, isConnecting = false) }
                if (room != null && room.queue.isNotEmpty()) {
                    playerController.syncQueue(room.queue)
                    
                    // Check skip votes
                    val participantCount = room.participants.size.coerceAtLeast(1)
                    val skipThreshold = (participantCount / 2.0).let { java.lang.Math.ceil(it).toInt() }.coerceAtLeast(1)
                    if (room.skipVotes.size >= skipThreshold && room.hostId == jamSessionManager.currentUserId) {
                        forceSkip()
                    }
                }
            }
        }
        
        // Host queue sync: if playerController generates autoplay suggestions, push them to Firebase
        viewModelScope.launch {
            playerController.playerState.collectLatest { state ->
                val room = _uiState.value.activeRoom ?: return@collectLatest
                if (room.hostId == jamSessionManager.currentUserId) {
                    if (state.queue.size > room.queue.size) {
                        // Find newly added songs (autoplay suggestions)
                        val newSongs = state.queue.filter { s -> room.queue.none { it.id == s.id } }
                        for (song in newSongs) {
                            jamSessionManager.addToQueue(room.roomId, song)
                        }
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
