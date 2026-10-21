package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.AppContainer
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// Playlist Detail ViewModel
// -------------------------------------------------------------
data class PlaylistUiState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isDownloadingAll: Boolean = false,
    val downloadProgressText: String? = null,
    val isAllDownloaded: Boolean = false
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
                try {
                    val downloadedSongs = appContainer.manageDownloadsUseCase.getDownloaded().first()
                    val downloadedIds = downloadedSongs.map { it.id }.toSet()
                    val allDown = playlist.songs.isNotEmpty() && playlist.songs.all { it.id in downloadedIds || it.isDownloaded }
                    _uiState.update { it.copy(playlist = playlist, isLoading = false, isAllDownloaded = allDown) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(playlist = playlist, isLoading = false) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Playlist not found") }
            }
        }
    }

    fun downloadAllSongs() {
        val playlist = _uiState.value.playlist ?: return
        if (playlist.songs.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingAll = true) }
            val total = playlist.songs.size
            var completed = 0
            for ((index, song) in playlist.songs.withIndex()) {
                _uiState.update { it.copy(downloadProgressText = "${index + 1}/$total") }
                try {
                    appContainer.manageDownloadsUseCase.download(song)
                    completed++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _uiState.update {
                it.copy(
                    isDownloadingAll = false,
                    downloadProgressText = null,
                    isAllDownloaded = completed == total
                )
            }
        }
    }

    fun removeAllDownloads() {
        val playlist = _uiState.value.playlist ?: return
        viewModelScope.launch {
            for (song in playlist.songs) {
                try {
                    appContainer.manageDownloadsUseCase.remove(song.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _uiState.update { it.copy(isAllDownloaded = false) }
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

    fun playNext(song: Song) = playerController.playNext(song)
    fun addToQueue(song: Song) = playerController.addToQueue(song)

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val newFav = appContainer.manageFavoritesUseCase.toggleFavorite(song)
            if (playerController.playerState.value.currentSong?.id == song.id) {
                playerController.updateCurrentSong(song.copy(isFavorite = newFav))
            }
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            appContainer.manageDownloadsUseCase.download(song)
        }
    }

    fun removeSongFromPlaylist(songId: String) {
        viewModelScope.launch {
            appContainer.managePlaylistUseCase.removeSong(playlistId, songId)
            loadPlaylist()
        }
    }

    fun addSongToPlaylist(song: Song) {
        viewModelScope.launch {
            appContainer.managePlaylistUseCase.addSong(playlistId, song)
            loadPlaylist()
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null
    private val _searchResults = MutableStateFlow<List<Song>>(emptyList())
    val searchResults: StateFlow<List<Song>> = _searchResults.asStateFlow()

    fun searchSongs(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            try {
                val results = appContainer.searchMusicUseCase(query)
                _searchResults.value = results
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
