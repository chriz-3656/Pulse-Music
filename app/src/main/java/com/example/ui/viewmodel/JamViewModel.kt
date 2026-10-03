package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.JamSessionManager
import com.example.di.AppContainer
import com.example.domain.model.JamRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JamUiState(
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val activeRoom: JamRoom? = null
)

class JamViewModel(
    private val jamSessionManager: JamSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(JamUiState())
    val uiState: StateFlow<JamUiState> = _uiState.asStateFlow()

    fun createRoom(hostName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
            try {
                val roomCode = jamSessionManager.createRoom(hostName)
                observeRoom(roomCode)
            } catch (e: Exception) {
                _uiState.update { it.copy(isConnecting = false, errorMessage = e.message) }
            }
        }
    }

    fun joinRoom(roomCode: String, guestName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, errorMessage = null) }
            try {
                jamSessionManager.joinRoom(roomCode, guestName)
                observeRoom(roomCode)
            } catch (e: Exception) {
                _uiState.update { it.copy(isConnecting = false, errorMessage = e.message) }
            }
        }
    }

    fun leaveRoom() {
        val currentCode = _uiState.value.activeRoom?.roomId
        if (currentCode != null) {
            viewModelScope.launch {
                try {
                    jamSessionManager.leaveRoom(currentCode)
                } catch (e: Exception) {
                    // Ignore
                }
                _uiState.update { it.copy(activeRoom = null) }
            }
        }
    }

    private fun observeRoom(roomCode: String) {
        viewModelScope.launch {
            jamSessionManager.observeRoom(roomCode).collectLatest { room ->
                _uiState.update { it.copy(activeRoom = room, isConnecting = false) }
            }
        }
    }

    companion object {
        fun provideFactory(appContainer: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return JamViewModel(appContainer.jamSessionManager) as T
                }
            }
    }
}
