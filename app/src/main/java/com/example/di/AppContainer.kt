package com.example.di

import android.content.Context
import com.example.data.local.MusicDatabase
import com.example.data.player.MediaCacheManager
import com.example.data.player.TrackDownloadManager
import com.example.data.remote.MusicApiService
import com.example.data.remote.NetworkClient
import com.example.data.repository.MusicRepositoryImpl
import com.example.domain.repository.MusicRepository
import com.example.domain.usecase.GetLyricsUseCase
import com.example.domain.usecase.GetRecommendationsUseCase
import com.example.domain.usecase.GetSongDetailsUseCase
import com.example.domain.usecase.ManageDownloadsUseCase
import com.example.domain.usecase.ManageFavoritesUseCase
import com.example.domain.usecase.ManagePlaylistUseCase
import com.example.domain.usecase.ManageSettingsUseCase
import com.example.domain.usecase.SearchMusicUseCase
import com.example.player.MusicPlayerController
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient

class AppContainer(private val context: Context) {
    val database: MusicDatabase by lazy {
        MusicDatabase.getDatabase(context)
    }

    val moshi: Moshi by lazy {
        NetworkClient.createMoshi()
    }

    val apiOkHttpClient: OkHttpClient by lazy {
        NetworkClient.createApiOkHttpClient(context)
    }

    val mediaOkHttpClient: OkHttpClient by lazy {
        NetworkClient.createMediaOkHttpClient(context)
    }

    val okHttpClient: OkHttpClient by lazy {
        apiOkHttpClient
    }

    val apiService: MusicApiService by lazy {
        NetworkClient.createApiService(apiOkHttpClient, moshi)
    }

    val mediaCacheManager: MediaCacheManager by lazy {
        MediaCacheManager(context, mediaOkHttpClient)
    }

    val downloadManager: TrackDownloadManager by lazy {
        TrackDownloadManager(context, mediaOkHttpClient, database.songDao())
    }

    val musicRepository: MusicRepository by lazy {
        MusicRepositoryImpl(
            context = context,
            apiService = apiService,
            songDao = database.songDao(),
            playlistDao = database.playlistDao(),
            albumDao = database.albumDao(),
            artistDao = database.artistDao(),
            recentSearchDao = database.recentSearchDao(),
            mediaCacheManager = mediaCacheManager,
            downloadManager = downloadManager,
            moshi = moshi
        )
    }

    val playerController: MusicPlayerController by lazy {
        MusicPlayerController(context)
    }

    // Use cases
    val searchMusicUseCase by lazy { SearchMusicUseCase(musicRepository) }
    val getRecommendationsUseCase by lazy { GetRecommendationsUseCase(musicRepository) }
    val getSongDetailsUseCase by lazy { GetSongDetailsUseCase(musicRepository) }
    val getLyricsUseCase by lazy { GetLyricsUseCase(musicRepository) }
    val managePlaylistUseCase by lazy { ManagePlaylistUseCase(musicRepository) }
    val manageFavoritesUseCase by lazy { ManageFavoritesUseCase(musicRepository) }
    val manageDownloadsUseCase by lazy { ManageDownloadsUseCase(musicRepository) }
    val manageSettingsUseCase by lazy { ManageSettingsUseCase(musicRepository) }
}
