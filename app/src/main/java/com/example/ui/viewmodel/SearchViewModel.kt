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
// Search ViewModel
// -------------------------------------------------------------
enum class SearchFilter { ALL, DISCOVER, SONGS, ALBUMS, PLAYLISTS, ARTISTS }

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.ALL,
    val searchResults: SearchResults = SearchResults(),
    val searchSuggestions: List<String> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
    val currentProvider: MusicProvider = MusicProvider.AUTO
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val appContainer: AppContainer,
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val searchMusicUseCase = appContainer.searchMusicUseCase
    private val musicRepository = appContainer.musicRepository

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryInput = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchMusicUseCase.getRecentSearches().collectLatest { recent ->
                _uiState.update { it.copy(recentSearches = recent) }
            }
        }

        viewModelScope.launch {
            musicRepository.getUserSettings().collectLatest { settings ->
                _uiState.update { it.copy(currentProvider = settings.provider) }
            }
        }

        viewModelScope.launch {
            queryInput
                .debounce(350)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isNotBlank()) {
                        fetchSuggestions(query)
                    } else {
                        _uiState.update { it.copy(searchResults = SearchResults(), isSearching = false) }
                    }
                }
        }
    }

    fun setProvider(provider: MusicProvider) {
        viewModelScope.launch {
            musicRepository.setProvider(provider)
            val currentQ = _uiState.value.query
            if (currentQ.isNotBlank()) {
                performSearch(currentQ)
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, searchResults = SearchResults()) }
        queryInput.value = newQuery
    }

    private fun fetchSuggestions(query: String) {
        viewModelScope.launch {
            try {
                val suggestions = searchMusicUseCase.getSuggestions(query)
                _uiState.update { it.copy(searchSuggestions = suggestions, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(searchSuggestions = emptyList()) }
            }
        }
    }

    fun setFilter(filter: SearchFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun onSearchSubmitted(query: String) {
        if (query.isNotBlank()) {
            _uiState.update { it.copy(searchSuggestions = emptyList()) }
            viewModelScope.launch {
                searchMusicUseCase.saveSearch(query)
            }
            performSearch(query)
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            try {
                val results = musicRepository.searchAll(query)
                searchMusicUseCase.saveSearch(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearching = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            searchMusicUseCase.deleteSearch(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchMusicUseCase.clearHistory()
        }
    }

    fun playSongAsRadio(song: Song) {
        playerController.playRadio(song)
    }

    fun playSong(song: Song, queue: List<Song>? = null) {
        playerController.playSong(song, queue)
    }

    fun playNext(song: Song) {
        playerController.playNext(song)
    }

    fun addToQueue(song: Song) {
        playerController.addToQueue(song)
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            appContainer.manageDownloadsUseCase.download(song)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            appContainer.manageFavoritesUseCase.toggleFavorite(song)
        }
    }
}
