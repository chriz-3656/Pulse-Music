package com.example.domain.usecase

import com.example.domain.model.Album
import com.example.domain.model.MusicProvider
import com.example.domain.model.Playlist
import com.example.domain.model.ProviderStatus
import com.example.domain.model.Song
import com.example.domain.model.UserSettings
import com.example.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow

class SearchMusicUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(query: String): List<Song> = repository.searchSongs(query)
    suspend fun getSuggestions(query: String): List<String> = repository.getSearchSuggestions(query)
    fun getRecentSearches(): Flow<List<String>> = repository.getRecentSearches()
    suspend fun saveSearch(query: String) = repository.saveSearchQuery(query)
    suspend fun deleteSearch(query: String) = repository.deleteSearchQuery(query)
    suspend fun clearHistory() = repository.clearSearchHistory()
}

class GetRecommendationsUseCase(private val repository: MusicRepository) {
    fun getTrending(): Flow<List<Song>> = repository.getTrendingSongs()
    fun getAlbums(): Flow<List<Album>> = repository.getFeaturedAlbums()
    fun getPlaylists(): Flow<List<Playlist>> = repository.getFeaturedPlaylists()
}

class GetSongDetailsUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(id: String): Result<Song> = repository.getSongDetails(id)
    suspend fun resolveStream(song: Song): String = repository.resolveStreamUrl(song)
}

class GetLyricsUseCase(private val repository: MusicRepository) {
    suspend operator fun invoke(id: String): Result<String> = repository.getLyrics(id)
}

class ManagePlaylistUseCase(private val repository: MusicRepository) {
    fun getUserPlaylists(): Flow<List<Playlist>> = repository.getUserPlaylists()
    fun getPlaylistSongs(playlistId: String): Flow<List<Song>> = repository.getPlaylistSongs(playlistId)
    suspend fun createPlaylist(title: String, description: String): String = repository.createPlaylist(title, description)
    suspend fun deletePlaylist(playlistId: String) = repository.deletePlaylist(playlistId)
    suspend fun addSong(playlistId: String, song: Song) = repository.addSongToPlaylist(playlistId, song)
    suspend fun removeSong(playlistId: String, songId: String) = repository.removeSongFromPlaylist(playlistId, songId)
    fun importSpotifyPlaylist(url: String) = repository.importSpotifyPlaylist(url)
}

class ManageFavoritesUseCase(private val repository: MusicRepository) {
    fun getFavorites(): Flow<List<Song>> = repository.getFavoriteSongs()
    suspend fun toggleFavorite(song: Song) = repository.toggleFavorite(song)
}

class ManageDownloadsUseCase(private val repository: MusicRepository) {
    fun getDownloaded(): Flow<List<Song>> = repository.getDownloadedSongs()
    suspend fun download(song: Song): Result<String> = repository.downloadSong(song)
    suspend fun remove(songId: String) = repository.removeDownload(songId)
}

class ManageSettingsUseCase(private val repository: MusicRepository) {
    fun getSettings(): Flow<UserSettings> = repository.getUserSettings()
    suspend fun updateSettings(settings: UserSettings) = repository.updateSettings(settings)
    suspend fun setProvider(provider: MusicProvider) = repository.setProvider(provider)
    suspend fun checkAllProviders(): Map<MusicProvider, ProviderStatus> = repository.checkAllProviders()
    fun getCacheSizeBytes(): Long = repository.getCacheSizeBytes()
    suspend fun clearCache() = repository.clearCache()
    suspend fun testApiEndpoint(url: String): Result<Long> = repository.testApiEndpoint(url)
}
