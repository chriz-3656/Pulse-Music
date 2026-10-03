package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.di.AppContainer
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.AudioQuality
import com.example.domain.model.MusicProvider
import com.example.domain.model.PlayerState
import com.example.domain.model.Playlist
import com.example.domain.model.ImportProgress
import com.example.domain.model.ProviderStatus
import com.example.domain.model.Song
import com.example.domain.model.UserSettings
import com.example.domain.repository.SearchResults
import com.example.domain.usecase.GetLyricsUseCase
import com.example.domain.usecase.GetRecommendationsUseCase
import com.example.domain.usecase.GetSongDetailsUseCase
import com.example.domain.usecase.ManageDownloadsUseCase
import com.example.domain.usecase.ManageFavoritesUseCase
import com.example.domain.usecase.ManagePlaylistUseCase
import com.example.domain.usecase.ManageSettingsUseCase
import com.example.domain.usecase.SearchMusicUseCase
import com.example.player.MusicPlayerController
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// Player ViewModel
// -------------------------------------------------------------
data class PlayerUiState(
    val playerState: PlayerState = PlayerState(),
    val lyrics: String? = null,
    val isLyricsLoading: Boolean = false,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val downloadInProgress: Boolean = false,
    val showQueueSheet: Boolean = false,
    val showLyricsSheet: Boolean = false
) {
    val showQueue: Boolean get() = showQueueSheet
    val showLyrics: Boolean get() = showLyricsSheet
    val lyricsLoading: Boolean get() = isLyricsLoading
}

class PlayerViewModel(
    val playerController: MusicPlayerController,
    private val getLyricsUseCase: GetLyricsUseCase,
    private val manageFavoritesUseCase: ManageFavoritesUseCase,
    private val manageDownloadsUseCase: ManageDownloadsUseCase
) : ViewModel() {

    val playerState = playerController.playerState

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerController.playerState.collectLatest { state ->
                val song = state.currentSong
                _uiState.update {
                    it.copy(
                        playerState = state,
                        isFavorite = song?.isFavorite ?: false,
                        isDownloaded = song?.isDownloaded ?: false,
                        lyrics = if (it.playerState?.currentSong?.id != song?.id) null else it.lyrics
                    )
                }
                if (song != null && _uiState.value.lyrics == null) {
                    loadLyricsForSong(song)
                }
            }
        }
    }

    fun loadLyricsForSong(song: Song) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLyricsLoading = true) }
            val result = getLyricsUseCase(song.id)
            _uiState.update {
                it.copy(
                    lyrics = result.getOrNull() ?: song.lyrics ?: "No lyrics available for this track.",
                    isLyricsLoading = false
                )
            }
        }
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun seekTo(posMs: Long) = playerController.seekTo(posMs)
    fun skipToNext() = playerController.skipToNext()
    fun skipToPrevious() = playerController.skipToPrevious()
    fun toggleShuffle() = playerController.toggleShuffle()
    fun toggleRepeat() = playerController.toggleRepeatMode()
    fun toggleRepeatMode() = playerController.toggleRepeatMode()
    fun reorderQueue(from: Int, to: Int) = playerController.reorderQueue(from, to)
    fun removeFromQueue(index: Int) = playerController.removeFromQueue(index)
    fun clearQueue() = playerController.clearQueue()
    fun playQueueIndex(index: Int) {
        val q = playerState.value.queue
        if (index in q.indices) {
            playerController.playQueue(q, index)
        }
    }

    fun toggleFavorite() {
        val song = playerController.playerState.value.currentSong ?: return
        viewModelScope.launch {
            manageFavoritesUseCase.toggleFavorite(song)
            _uiState.update { it.copy(isFavorite = !it.isFavorite) }
        }
    }

    fun downloadTrack() {
        val song = playerController.playerState.value.currentSong ?: return
        viewModelScope.launch {
            if (_uiState.value.isDownloaded) {
                // Toggle / delete download
                manageDownloadsUseCase.remove(song.id)
                _uiState.update { it.copy(isDownloaded = false) }
                playerController.updateCurrentSong(song.copy(isDownloaded = false, localFilePath = null))
            } else {
                _uiState.update { it.copy(downloadInProgress = true) }
                val result = manageDownloadsUseCase.download(song)
                val success = result.isSuccess
                _uiState.update {
                    it.copy(
                        downloadInProgress = false,
                        isDownloaded = success
                    )
                }
                if (success) {
                    val localPath = result.getOrNull()
                    playerController.updateCurrentSong(song.copy(isDownloaded = true, localFilePath = localPath))
                }
            }
        }
    }

    fun toggleQueueSheet(show: Boolean) {
        _uiState.update { it.copy(showQueueSheet = show) }
    }

    fun toggleLyricsSheet(show: Boolean) {
        _uiState.update { it.copy(showLyricsSheet = show) }
    }

    fun toggleQueue() {
        _uiState.update { it.copy(showQueueSheet = !it.showQueueSheet) }
    }

    fun toggleLyrics() {
        _uiState.update { it.copy(showLyricsSheet = !it.showLyricsSheet) }
    }

    fun setSleepTimer(minutes: Int) {
        playerController.setSleepTimer(minutes)
    }
}
