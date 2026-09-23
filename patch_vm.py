import re

with open("app/src/main/java/com/example/ui/viewmodel/MusicViewModels.kt", "r") as f:
    text = f.read()

# Add ImportProgress import if missing
if "com.example.domain.model.ImportProgress" not in text:
    text = text.replace("import com.example.domain.model.Playlist", "import com.example.domain.model.Playlist\nimport com.example.domain.model.ImportProgress")

ui_state_regex = r'data class LibraryUiState\([\s\S]*?\)'
# Need to find LibraryUiState and add importProgress
new_ui_state = """data class LibraryUiState(
    val playlists: List<Playlist> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val downloadedSongs: List<Song> = emptyList(),
    val importProgress: ImportProgress? = null
)"""
text = re.sub(ui_state_regex, new_ui_state, text)

# Add import function to LibraryViewModel
vm_logic = """    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            managePlaylistUseCase.removeSong(playlistId, songId)
        }
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

text = text.replace("""    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            managePlaylistUseCase.removeSong(playlistId, songId)
        }
    }""", vm_logic)

with open("app/src/main/java/com/example/ui/viewmodel/MusicViewModels.kt", "w") as f:
    f.write(text)
