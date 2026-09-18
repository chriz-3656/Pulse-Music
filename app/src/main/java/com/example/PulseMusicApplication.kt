package com.example

import android.app.Application
import com.example.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PulseMusicApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
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
            appContainer.musicRepository.getUserSettings().collectLatest { settings ->
                appContainer.playerController.isAutoplayEnabled = settings.autoplayEnabled
                appContainer.playerController.setAudioQuality(settings.audioQuality)
                appContainer.playerController.setCrossfade(settings.crossfadeDurationSec)
            }
        }
    }
}
