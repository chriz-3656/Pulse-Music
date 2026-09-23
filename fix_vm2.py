import re

with open("app/src/main/java/com/example/ui/viewmodel/MusicViewModels.kt", "r") as f:
    text = f.read()

new_methods = """    fun playSong(song: Song, queue: List<Song>) {
        playerController.playSong(song, queue)
    }

    fun importSpotifyPlaylist(url: String) {
        viewModelScope.launch {
            managePlaylistUseCase.importSpotifyPlaylist(url).collect { progress ->
                _uiState.update { it.copy(importProgress = progress) }
                if (progress.isComplete) {
                    kotlinx.coroutines.delay(3000)
                    _uiState.update { it.copy(importProgress = null) }
                }
            }
        }
    }
    
    fun dismissImportDialog() {
        _uiState.update { it.copy(importProgress = null) }
    }"""

text = text.replace("""    fun playSong(song: Song, queue: List<Song>) {
        playerController.playSong(song, queue)
    }""", new_methods)

with open("app/src/main/java/com/example/ui/viewmodel/MusicViewModels.kt", "w") as f:
    f.write(text)
