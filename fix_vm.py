import re

with open("app/src/main/java/com/example/ui/viewmodel/MusicViewModels.kt", "r") as f:
    text = f.read()

bad_state = """data class LibraryUiState(
    val playlists: List<Playlist> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val downloadedSongs: List<Song> = emptyList(),
    val importProgress: ImportProgress? = null
),
    val favoriteSongs: List<Song> = emptyList(),
    val downloadedSongs: List<Song> = emptyList(),
    val showCreateDialog: Boolean = false,
    val isLoading: Boolean = false
)"""

good_state = """data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.PLAYLISTS,
    val playlists: List<Playlist> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val downloadedSongs: List<Song> = emptyList(),
    val showCreateDialog: Boolean = false,
    val isLoading: Boolean = false,
    val importProgress: ImportProgress? = null
)"""

text = text.replace(bad_state, good_state)

with open("app/src/main/java/com/example/ui/viewmodel/MusicViewModels.kt", "w") as f:
    f.write(text)
