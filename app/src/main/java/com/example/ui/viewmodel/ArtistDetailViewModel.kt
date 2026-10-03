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
// Artist Detail ViewModel
// -------------------------------------------------------------
data class ArtistUiState(
    val artist: Artist? = null,
    val topSongs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ArtistDetailViewModel(
    private val artistId: String,
    private val appContainer: AppContainer
) : ViewModel() {

    private val repository = appContainer.musicRepository
    private val playerController = appContainer.playerController

    private val _uiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    init {
        loadArtistData()
    }

    fun loadArtistData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val profileResult = repository.getArtistProfile(artistId)
            val artist = profileResult.getOrNull()
            if (artist != null) {
                val songs = if (artist.topSongs.isNotEmpty()) artist.topSongs else repository.getArtistSongs(artistId)
                val albums = if (artist.topAlbums.isNotEmpty()) artist.topAlbums else repository.getArtistAlbums(artistId)
                _uiState.update {
                    it.copy(
                        artist = artist,
                        topSongs = songs,
                        albums = albums,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Artist not found"
                    )
                }
            }
        }
    }

    fun playSong(song: Song) {
        playerController.playSong(song, _uiState.value.topSongs)
    }

    fun playTrack(song: Song) = playSong(song)

    fun playAll() = playAllSongs()

    fun playAllSongs() {
        val songs = _uiState.value.topSongs
        if (songs.isNotEmpty()) {
            playerController.playQueue(songs, 0)
        }
    }

    fun shuffleAll() {
        val songs = _uiState.value.topSongs.shuffled()
        if (songs.isNotEmpty()) {
            playerController.playQueue(songs, 0)
        }
    }
}
