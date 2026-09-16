package com.example.data.remote

import com.example.data.remote.dto.AlbumDto
import com.example.data.remote.dto.ArtistDto
import com.example.data.remote.dto.LyricsDto
import com.example.data.remote.dto.PlaylistDto
import com.example.data.remote.dto.RecommendationsDto
import com.example.data.remote.dto.SearchResponseDto
import com.example.data.remote.dto.SongDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MusicApiService {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String? = null
    ): Response<SearchResponseDto>

    @GET("search/all")
    suspend fun searchAll(
        @Query("query") query: String
    ): Response<SearchResponseDto>

    @GET("search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int? = 20
    ): Response<Any>

    @GET("search/albums")
    suspend fun searchAlbums(
        @Query("query") query: String,
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int? = 20
    ): Response<Any>

    @GET("search/playlists")
    suspend fun searchPlaylists(
        @Query("query") query: String,
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int? = 20
    ): Response<Any>

    @GET("search/artists")
    suspend fun searchArtists(
        @Query("query") query: String,
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int? = 20
    ): Response<Any>

    @GET("songs/{id}")
    suspend fun getSongById(
        @Path("id") id: String
    ): Response<Any>

    @GET("song/{id}")
    suspend fun getSongLegacy(
        @Path("id") id: String
    ): Response<SongDto>

    @GET("songs")
    suspend fun getSongsBatch(
        @Query("ids") ids: String? = null,
        @Query("id") id: String? = null
    ): Response<Any>

    @GET("albums/{id}")
    suspend fun getAlbumById(
        @Path("id") id: String
    ): Response<Any>

    @GET("album/{id}")
    suspend fun getAlbumLegacy(
        @Path("id") id: String
    ): Response<AlbumDto>

    @GET("playlists/{id}")
    suspend fun getPlaylistById(
        @Path("id") id: String
    ): Response<Any>

    @GET("playlist/{id}")
    suspend fun getPlaylistLegacy(
        @Path("id") id: String
    ): Response<PlaylistDto>

    @GET("artists/{id}")
    suspend fun getArtistById(
        @Path("id") id: String
    ): Response<Any>

    @GET("artists/{id}/songs")
    suspend fun getArtistSongs(
        @Path("id") id: String,
        @Query("page") page: Int? = null,
        @Query("count") count: Int? = 30
    ): Response<Any>

    @GET("artists/{id}/albums")
    suspend fun getArtistAlbums(
        @Path("id") id: String,
        @Query("page") page: Int? = null,
        @Query("count") count: Int? = 30
    ): Response<Any>

    @GET("songs/{id}/suggestions")
    suspend fun getSongSuggestions(
        @Path("id") id: String
    ): Response<Any>

    @GET("lyrics/{id}")
    suspend fun getLyrics(
        @Path("id") id: String
    ): Response<LyricsDto>

    @GET("lyrics")
    suspend fun getLyricsByQuery(
        @Query("id") id: String
    ): Response<LyricsDto>

    @GET("recommendations")
    suspend fun getRecommendations(): Response<RecommendationsDto>

    @GET("trending")
    suspend fun getTrending(): Response<List<SongDto>>

    @GET("modules")
    suspend fun getModules(
        @Query("language") language: String? = "english,hindi"
    ): Response<Any>
}
