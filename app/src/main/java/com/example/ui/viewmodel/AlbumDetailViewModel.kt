package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.AppContainer
import com.example.domain.model.Album
import com.example.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// Album Detail ViewModel
// -------------------------------------------------------------
data class AlbumUiState(
    val album: Album? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isDownloadingAll: Boolean = false,
    val downloadProgressText: String? = null,
    val isAllDownloaded: Boolean = false
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
        viewModelScope.launch {
            appContainer.manageDownloadsUseCase.getDownloadTasks().collectLatest { tasks ->
                val album = _uiState.value.album ?: return@collectLatest
                val songIds = album.songs.map { it.id }.toSet()
                val related = tasks.filter { it.song.id in songIds }
                if (related.isNotEmpty()) {
                    val completed = album.songs.size - related.size
                    _uiState.update {
                        it.copy(
                            isDownloadingAll = true,
                            downloadProgressText = "$completed/${album.songs.size}"
                        )
                    }
                } else if (_uiState.value.isDownloadingAll) {
                    val downloadedSongs = appContainer.manageDownloadsUseCase.getDownloaded().first()
                    val downloadedIds = downloadedSongs.map { it.id }.toSet()
                    val allDown = album.songs.isNotEmpty() && album.songs.all { it.id in downloadedIds || it.isDownloaded }
                    _uiState.update {
                        it.copy(
                            isDownloadingAll = false,
                            downloadProgressText = null,
                            isAllDownloaded = allDown
                        )
                    }
                }
            }
        }
    }

    fun loadAlbum() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getAlbumDetails(albumId)
            val album = result.getOrNull()
            if (album != null) {
                try {
                    val downloadedSongs = appContainer.manageDownloadsUseCase.getDownloaded().first()
                    val downloadedIds = downloadedSongs.map { it.id }.toSet()
                    val allDown = album.songs.isNotEmpty() && album.songs.all { it.id in downloadedIds || it.isDownloaded }
                    _uiState.update { it.copy(album = album, isLoading = false, isAllDownloaded = allDown) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(album = album, isLoading = false) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Album not found") }
            }
        }
    }

    fun downloadAllSongs() {
        val album = _uiState.value.album ?: return
        if (album.songs.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingAll = true, downloadProgressText = "0/${album.songs.size}") }
            appContainer.manageDownloadsUseCase.enqueueDownloads(
                album.songs,
                sourceCollection = "Album: ${album.title}"
            )
        }
    }

    fun removeAllDownloads() {
        val album = _uiState.value.album ?: return
        viewModelScope.launch {
            for (song in album.songs) {
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
