package com.example.domain.repository

import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.MusicProvider
import com.example.domain.model.Playlist
import com.example.domain.model.ProviderStatus
import com.example.domain.model.Song
import com.example.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val artistTopSongs: List<Song> = emptyList(),
    val similarTracks: List<Song> = emptyList(),
    val genreRecommendations: List<Song> = emptyList(),
    val fansAlsoLikedArtists: List<Artist> = emptyList(),
    val detectedArtistName: String? = null,
    val detectedGenre: String? = null
)

interface MusicRepository {
    // Search
    suspend fun searchSongs(query: String): List<Song>
    suspend fun searchAll(query: String): SearchResults
    suspend fun getSearchSuggestions(query: String): List<String>
    fun getRecentSearches(): Flow<List<String>>
    suspend fun saveSearchQuery(query: String)
    suspend fun deleteSearchQuery(query: String)
    suspend fun clearSearchHistory()

    // Songs & Suggestions
    suspend fun getSongDetails(id: String): Result<Song>
    suspend fun resolveStreamUrl(song: Song): String
    suspend fun getSongsBatch(ids: List<String>): List<Song>
    suspend fun getLyrics(id: String): Result<String>
    suspend fun getSongSuggestions(songId: String): List<Song>
    suspend fun getSongSuggestions(song: Song): List<Song>
    
    // Albums & Playlists Details
    suspend fun getAlbumDetails(id: String): Result<Album>
    suspend fun getPlaylistDetails(id: String): Result<Playlist>
    
    // Artist Profiles
    suspend fun getArtistProfile(id: String): Result<Artist>
    suspend fun getArtistSongs(artistId: String): List<Song>
    suspend fun getArtistAlbums(artistId: String): List<Album>

    // Discovery & Feeds
    fun getTrendingSongs(): Flow<List<Song>>
    fun getFeaturedAlbums(): Flow<List<Album>>
    fun getFeaturedPlaylists(): Flow<List<Playlist>>

    // User Playlists
    fun getUserPlaylists(): Flow<List<Playlist>>
    fun getPlaylistSongs(playlistId: String): Flow<List<Song>>
    suspend fun createPlaylist(title: String, description: String): String
    suspend fun deletePlaylist(playlistId: String)
    suspend fun addSongToPlaylist(playlistId: String, song: Song)
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)
    fun importSpotifyPlaylist(url: String): Flow<com.example.domain.model.ImportProgress>

    // Favorites & Downloads
    fun getFavoriteSongs(): Flow<List<Song>>
    suspend fun toggleFavorite(song: Song)
    fun getDownloadedSongs(): Flow<List<Song>>
    suspend fun downloadSong(song: Song): Result<String>
    suspend fun removeDownload(songId: String)
    suspend fun getLocalDownloadPath(songId: String): String?

    // Cache & Settings
    fun getUserSettings(): Flow<UserSettings>
    suspend fun updateSettings(settings: UserSettings)
    suspend fun setProvider(provider: MusicProvider)
    suspend fun checkAllProviders(): Map<MusicProvider, ProviderStatus>
    fun getCacheSizeBytes(): Long
    suspend fun clearCache()
    suspend fun testApiEndpoint(url: String): Result<Long>
}
