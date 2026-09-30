package com.example.data.remote

import com.squareup.moshi.JsonClass
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

    @GET("v1/me")
    suspend fun getCurrentUserProfile(
        @Header("Authorization") authHeader: String
    ): SpotifyUserDto

    @GET("v1/me/tracks")
    suspend fun getMyLikedSongs(
        @Header("Authorization") authHeader: String,
        @Query("limit") limit: Int = 1,
        @Query("offset") offset: Int = 0
    ): SpotifyTracksInfoResponse
}


@JsonClass(generateAdapter = true)
data class SpotifyPlaylistResponse(
    val items: List<SpotifyPlaylistDto>
)


@JsonClass(generateAdapter = true)
data class SpotifyPlaylistDto(
    val id: String,
    val name: String,
    val images: List<SpotifyImageDto>?,
    val tracks: SpotifyTracksInfo
)


@JsonClass(generateAdapter = true)
data class SpotifyImageDto(
    val url: String
)


@JsonClass(generateAdapter = true)
data class SpotifyTracksInfo(
    val total: Int
)


@JsonClass(generateAdapter = true)
data class SpotifyTracksResponse(
    val items: List<SpotifyTrackItemDto>
)


@JsonClass(generateAdapter = true)
data class SpotifyTrackItemDto(
    val track: SpotifyTrackDto
)


@JsonClass(generateAdapter = true)
data class SpotifyTrackDto(
    val id: String,
    val name: String,
    val artists: List<SpotifyArtistDto>,
    val album: SpotifyAlbumDto,
    val duration_ms: Long
)


@JsonClass(generateAdapter = true)
data class SpotifyArtistDto(
    val name: String
)


@JsonClass(generateAdapter = true)
data class SpotifyAlbumDto(
    val name: String,
    val images: List<SpotifyImageDto>?
)


@JsonClass(generateAdapter = true)
data class SpotifyUserDto(
    val id: String,
    val display_name: String?,
    val email: String?,
    val images: List<SpotifyImageDto>?
)


@JsonClass(generateAdapter = true)
data class SpotifyTracksInfoResponse(
    val total: Int
)
