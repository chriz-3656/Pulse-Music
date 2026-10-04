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
import com.example.ui.viewmodel.JamViewModel
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
    val isCheckingProviders: Boolean = false,
    val isDeveloperMode: Boolean = false
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
            playerController.setAudioQuality(_uiState.value.userSettings.audioQuality)
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

    fun setDeveloperMode(enabled: Boolean) {
        _uiState.update { it.copy(isDeveloperMode = enabled) }
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
                SettingsViewModel(
                    appContainer.manageSettingsUseCase, 
                    appContainer.playerController
                ) as T
            }
            modelClass.isAssignableFrom(JamViewModel::class.java) -> {
                JamViewModel(appContainer.jamSessionManager, appContainer.searchMusicUseCase, appContainer.playerController) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
