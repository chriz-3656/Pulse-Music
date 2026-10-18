package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ImportProgress
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.domain.usecase.ManageDownloadsUseCase
import com.example.domain.usecase.ManageFavoritesUseCase
import com.example.domain.usecase.ManagePlaylistUseCase
import com.example.player.MusicPlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// Library ViewModel
// -------------------------------------------------------------
enum class LibraryTab { PLAYLISTS, FAVORITES, DOWNLOADS }

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.PLAYLISTS,
    val playlists: List<Playlist> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val downloadedSongs: List<Song> = emptyList(),
    val showCreateDialog: Boolean = false,
    val isLoading: Boolean = false,
    val importProgress: ImportProgress? = null,
)

class LibraryViewModel(
    private val managePlaylistUseCase: ManagePlaylistUseCase,
    private val manageFavoritesUseCase: ManageFavoritesUseCase,
    private val manageDownloadsUseCase: ManageDownloadsUseCase,
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                managePlaylistUseCase.getUserPlaylists().collectLatest { pls ->
                    _uiState.update { it.copy(playlists = pls) }
                }
            }
            launch {
                manageFavoritesUseCase.getFavorites().collectLatest { favs ->
                    _uiState.update { it.copy(favoriteSongs = favs) }
                }
            }
            launch {
                manageDownloadsUseCase.getDownloaded().collectLatest { downs ->
                    _uiState.update { it.copy(downloadedSongs = downs) }
                }
            }
        }
    }

    fun setTab(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun showCreatePlaylistDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateDialog = show) }
    }

    fun createPlaylist(title: String, desc: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            managePlaylistUseCase.createPlaylist(title, desc)
            _uiState.update { it.copy(showCreateDialog = false) }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            managePlaylistUseCase.deletePlaylist(playlistId)
        }
    }

    fun playSong(song: Song, queue: List<Song>) {
        playerController.playSong(song, queue)
    }

    fun importSpotifyPlaylist(url: String) {
        viewModelScope.launch {
            managePlaylistUseCase.importSpotifyPlaylist(url).collect { progress ->
                _uiState.update { it.copy(importProgress = progress) }
                if (progress.isComplete) {
                    kotlinx.coroutines.delay(3000)
                    _uiState.update { it.copy(importProgress = null) }
                }
            }
        }
    }
    
    fun dismissImportDialog() {
        _uiState.update { it.copy(importProgress = null) }
    }

    fun playAll(queue: List<Song>) {
        if (queue.isNotEmpty()) {
            playerController.playQueue(queue, 0)
        }
    }

    fun removeDownload(songId: String) {
        viewModelScope.launch {
            manageDownloadsUseCase.remove(songId)
        }
    }

    fun playNext(song: Song) = playerController.playNext(song)
    fun addToQueue(song: Song) = playerController.addToQueue(song)
    fun downloadSong(song: Song) {
        viewModelScope.launch {
            manageDownloadsUseCase.download(song)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val newFav = manageFavoritesUseCase.toggleFavorite(song)
            if (playerController.playerState.value.currentSong?.id == song.id) {
                playerController.updateCurrentSong(song.copy(isFavorite = newFav))
            }
        }
    }
}
