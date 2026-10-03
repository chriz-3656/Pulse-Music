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
// Playlist Detail ViewModel
// -------------------------------------------------------------
data class PlaylistUiState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class PlaylistDetailViewModel(
    private val playlistId: String,
    private val appContainer: AppContainer
) : ViewModel() {

    private val repository = appContainer.musicRepository
    private val playerController = appContainer.playerController

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        loadPlaylist()
    }

    fun loadPlaylist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getPlaylistDetails(playlistId)
            val playlist = result.getOrNull()
            if (playlist != null) {
                _uiState.update { it.copy(playlist = playlist, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Playlist not found") }
            }
        }
    }

    fun playTrack(song: Song) {
        val playlist = _uiState.value.playlist ?: return
        playerController.playSong(song, playlist.songs)
    }

    fun playPlaylist() {
        val playlist = _uiState.value.playlist ?: return
        if (playlist.songs.isNotEmpty()) {
            playerController.playQueue(playlist.songs, 0)
        }
    }
}
