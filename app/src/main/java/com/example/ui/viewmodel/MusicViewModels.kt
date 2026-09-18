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
// Home ViewModel
// -------------------------------------------------------------
data class HomeUiState(
    val trendingSongs: List<Song> = emptyList(),
    val featuredAlbums: List<Album> = emptyList(),
    val featuredPlaylists: List<Playlist> = emptyList(),
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
                    getRecommendationsUseCase.getTrending().collectLatest { songs ->
                        _uiState.update { it.copy(trendingSongs = songs, isLoading = false) }
                    }
                }
                launch {
                    getRecommendationsUseCase.getAlbums().collectLatest { albums ->
                        _uiState.update { it.copy(featuredAlbums = albums) }
                    }
                }
                launch {
                    getRecommendationsUseCase.getPlaylists().collectLatest { playlists ->
                        _uiState.update { it.copy(featuredPlaylists = playlists) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
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
}

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
    val isLoading: Boolean = false
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

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            manageFavoritesUseCase.toggleFavorite(song)
        }
    }
}

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

// -------------------------------------------------------------
// Settings ViewModel
// -------------------------------------------------------------
sealed class ApiEndpointStatus {
    object Idle : ApiEndpointStatus()
    object Testing : ApiEndpointStatus()
    data class Success(val latencyMs: Long) : ApiEndpointStatus()
    data class Error(val message: String) : ApiEndpointStatus()
}

data class SettingsUiState(
    val userSettings: UserSettings = UserSettings(),
    val cacheSizeBytes: Long = 0L,
    val showClearSuccessMessage: Boolean = false,
    val apiTestStatus: ApiEndpointStatus = ApiEndpointStatus.Idle,
    val customUrlDraft: String = "",
    val playerState: PlayerState = PlayerState(),
    val providerStatuses: Map<MusicProvider, ProviderStatus> = emptyMap(),
    val isCheckingProviders: Boolean = false
)

class SettingsViewModel(
    private val manageSettingsUseCase: ManageSettingsUseCase,
    private val playerController: MusicPlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            manageSettingsUseCase.getSettings().collectLatest { settings ->
                playerController.isAutoplayEnabled = settings.autoplayEnabled
                _uiState.update {
                    it.copy(
                        userSettings = settings,
                        cacheSizeBytes = manageSettingsUseCase.getCacheSizeBytes(),
                        customUrlDraft = settings.customApiBaseUrl
                    )
                }
            }
        }
        viewModelScope.launch {
            playerController.playerState.collectLatest { state ->
                _uiState.update { it.copy(playerState = state) }
            }
        }
        checkProviders()
    }

    fun setProvider(provider: MusicProvider) {
        viewModelScope.launch {
            manageSettingsUseCase.setProvider(provider)
        }
    }

    fun checkProviders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingProviders = true) }
            val statuses = manageSettingsUseCase.checkAllProviders()
            _uiState.update { it.copy(providerStatuses = statuses, isCheckingProviders = false) }
        }
    }

    fun setQuality(quality: AudioQuality) {
        val updated = _uiState.value.userSettings.copy(audioQuality = quality)
        viewModelScope.launch {
            manageSettingsUseCase.updateSettings(updated)
            playerController.setAudioQuality(quality)
        }
    }

    fun setCacheLimit(limitMb: Int) {
        val updated = _uiState.value.userSettings.copy(cacheSizeLimitMb = limitMb)
        viewModelScope.launch {
            manageSettingsUseCase.updateSettings(updated)
        }
    }

    fun setCrossfadeDuration(sec: Int) {
        val updated = _uiState.value.userSettings.copy(crossfadeDurationSec = sec)
        viewModelScope.launch {
            manageSettingsUseCase.updateSettings(updated)
            playerController.setCrossfade(sec)
        }
    }

    fun toggleDarkMode(isDark: Boolean) {
        val updated = _uiState.value.userSettings.copy(isDarkTheme = isDark)
        viewModelScope.launch {
            manageSettingsUseCase.updateSettings(updated)
        }
    }

    fun toggleAutoSkip(autoSkip: Boolean) {
        val updated = _uiState.value.userSettings.copy(autoSkipFailedTracks = autoSkip)
        viewModelScope.launch {
            manageSettingsUseCase.updateSettings(updated)
        }
    }

    fun toggleAutoplay(enabled: Boolean) {
        val updated = _uiState.value.userSettings.copy(autoplayEnabled = enabled)
        playerController.isAutoplayEnabled = enabled
        viewModelScope.launch {
            manageSettingsUseCase.updateSettings(updated)
        }
    }

    fun updateCustomUrlDraft(url: String) {
        _uiState.update { it.copy(customUrlDraft = url) }
    }

    fun saveApiEndpoint(provider: String, customUrl: String) {
        val updated = _uiState.value.userSettings.copy(
            apiProvider = provider,
            customApiBaseUrl = customUrl.trim()
        )
        viewModelScope.launch {
            manageSettingsUseCase.updateSettings(updated)
        }
    }

    fun testApiEndpoint(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(apiTestStatus = ApiEndpointStatus.Testing) }
            val result = manageSettingsUseCase.testApiEndpoint(url)
            result.fold(
                onSuccess = { latency ->
                    _uiState.update { it.copy(apiTestStatus = ApiEndpointStatus.Success(latency)) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(apiTestStatus = ApiEndpointStatus.Error(error.localizedMessage ?: "Failed to connect")) }
                }
            )
        }
    }

    fun resetApiEndpoint() {
        val updated = _uiState.value.userSettings.copy(
            apiProvider = "default",
            customApiBaseUrl = ""
        )
        _uiState.update { it.copy(customUrlDraft = "", apiTestStatus = ApiEndpointStatus.Idle) }
        viewModelScope.launch {
            manageSettingsUseCase.updateSettings(updated)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            manageSettingsUseCase.clearCache()
            _uiState.update {
                it.copy(
                    cacheSizeBytes = 0L,
                    showClearSuccessMessage = true
                )
            }
        }
    }

    fun dismissClearMessage() {
        _uiState.update { it.copy(showClearSuccessMessage = false) }
    }

    fun retryPlayback() {
        val current = _uiState.value.playerState.currentSong
        if (current != null) {
            val q = _uiState.value.playerState.queue
            val queueToUse = if (q.isNotEmpty()) q else listOf(current)
            playerController.playSong(current, queueToUse)
        }
    }
}

// -------------------------------------------------------------
// ViewModel Factory
// -------------------------------------------------------------
class ViewModelFactory(private val appContainer: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(appContainer.getRecommendationsUseCase, appContainer.playerController) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(appContainer, appContainer.playerController) as T
            }
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> {
                PlayerViewModel(
                    appContainer.playerController,
                    appContainer.getLyricsUseCase,
                    appContainer.manageFavoritesUseCase,
                    appContainer.manageDownloadsUseCase
                ) as T
            }
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> {
                LibraryViewModel(
                    appContainer.managePlaylistUseCase,
                    appContainer.manageFavoritesUseCase,
                    appContainer.manageDownloadsUseCase,
                    appContainer.playerController
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(appContainer.manageSettingsUseCase, appContainer.playerController) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
