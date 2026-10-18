package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.AppContainer
import com.example.domain.model.Album
import com.example.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// Album Detail ViewModel
// -------------------------------------------------------------
data class AlbumUiState(
    val album: Album? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class AlbumDetailViewModel(
    private val albumId: String,
    private val appContainer: AppContainer
) : ViewModel() {

    private val repository = appContainer.musicRepository
    private val playerController = appContainer.playerController

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init {
        loadAlbum()
    }

    fun loadAlbum() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getAlbumDetails(albumId)
            val album = result.getOrNull()
            if (album != null) {
                _uiState.update { it.copy(album = album, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Album not found") }
            }
        }
    }

    fun playTrack(song: Song) {
        val album = _uiState.value.album ?: return
        playerController.playSong(song, album.songs)
    }

    fun playAlbum() {
        val album = _uiState.value.album ?: return
        if (album.songs.isNotEmpty()) {
            playerController.playQueue(album.songs, 0)
        }
    }
}
