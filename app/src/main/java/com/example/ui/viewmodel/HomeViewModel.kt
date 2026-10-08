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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// Home ViewModel
// -------------------------------------------------------------
data class HomeUiState(
    val trendingSongs: List<Song> = emptyList(),
    val quickPicks: List<Song> = emptyList(),
    val featuredAlbums: List<Album> = emptyList(),
    val featuredPlaylists: List<Playlist> = emptyList(),
    val moodPlaylists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val getRecommendationsUseCase: GetRecommendationsUseCase,
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                launch {
                    getRecommendationsUseCase.getTrending().catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }.collectLatest { songs ->
                        _uiState.update { it.copy(trendingSongs = songs, isLoading = false) }
                    }
                }
                launch {
                    getRecommendationsUseCase.getTrending().catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }.collectLatest { songs ->
                        _uiState.update { it.copy(quickPicks = songs) }
                    }
                }
                launch {
                    getRecommendationsUseCase.getAlbums().catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }.collectLatest { albums ->
                        _uiState.update { it.copy(featuredAlbums = albums) }
                    }
                }
                launch {
                    getRecommendationsUseCase.getPlaylists().catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }.collectLatest { playlists ->
                        _uiState.update { it.copy(featuredPlaylists = playlists) }
                    }
                }
                launch {
                    getRecommendationsUseCase.getPlaylists().catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }.collectLatest { playlists ->
                        _uiState.update { it.copy(moodPlaylists = playlists) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun playSong(song: Song) {
        playerController.playSong(song, _uiState.value.trendingSongs)
    }

    fun playNext(song: Song) {
        playerController.playNext(song)
    }

    fun addToQueue(song: Song) {
        playerController.addToQueue(song)
    }

    fun playRadio(song: Song) {
        playerController.playRadio(song)
    }

    fun playAlbum(album: Album) {
        if (album.songs.isNotEmpty()) {
            playerController.playQueue(album.songs, 0)
        }
    }

    fun playPlaylist(playlist: Playlist) {
        if (playlist.songs.isNotEmpty()) {
            playerController.playQueue(playlist.songs, 0)
        }
    }
}
