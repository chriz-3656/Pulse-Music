package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface SpotifyApiService {
    @GET("v1/me/playlists")
    suspend fun getMyPlaylists(
        @Header("Authorization") authHeader: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): SpotifyPlaylistResponse

    @GET("v1/playlists/{playlist_id}/tracks")
    suspend fun getPlaylistTracks(
        @Header("Authorization") authHeader: String,
        @retrofit2.http.Path("playlist_id") playlistId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): SpotifyTracksResponse
}

data class SpotifyPlaylistResponse(
    val items: List<SpotifyPlaylistDto>
)

data class SpotifyPlaylistDto(
    val id: String,
    val name: String,
    val images: List<SpotifyImageDto>?,
    val tracks: SpotifyTracksInfo
)

data class SpotifyImageDto(
    val url: String
)

data class SpotifyTracksInfo(
    val total: Int
)

data class SpotifyTracksResponse(
    val items: List<SpotifyTrackItemDto>
)

data class SpotifyTrackItemDto(
    val track: SpotifyTrackDto
)

data class SpotifyTrackDto(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtistDto>,
    val album: SpotifyAlbumDto,
    val duration_ms: Long
)

data class SpotifyArtistDto(
    val name: String
)

data class SpotifyAlbumDto(
    val name: String,
    val images: List<SpotifyImageDto>?
)
