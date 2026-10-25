package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PulseMusicApplication : Application(), ImageLoaderFactory {
    lateinit var appContainer: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { appContainer.mediaOkHttpClient }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        org.schabi.newpipe.extractor.NewPipe.init(com.example.util.NewPipeDownloader())
        appContainer = AppContainer(this)
        
        // Connect both song-level and id-level autoplay providers
        appContainer.playerController.autoplaySongProvider = { song ->
            appContainer.musicRepository.getSongSuggestions(song)
        }
        appContainer.playerController.autoplayProvider = { songId ->
            appContainer.musicRepository.getSongSuggestions(songId)
        }

        // Sync settings (autoplay, audio quality, crossfade) with player controller
        appScope.launch {
            try {
                appContainer.musicRepository.getUserSettings().collectLatest { settings ->
                    appContainer.playerController.isAutoplayEnabled = settings.autoplayEnabled
                    appContainer.playerController.setAudioQuality(settings.audioQuality)
                    appContainer.playerController.setCrossfade(settings.crossfadeDurationSec)
                    appContainer.mediaCacheManager.setCacheLimitMb(settings.cacheSizeLimitMb.coerceIn(100, 5000))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
