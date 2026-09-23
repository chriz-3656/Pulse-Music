import re

with open("app/src/main/java/com/example/domain/repository/MusicRepository.kt", "r") as f:
    text = f.read()

new_method = """    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)
    fun importSpotifyPlaylist(url: String): Flow<com.example.domain.model.ImportProgress>"""

text = text.replace("    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)", new_method)

with open("app/src/main/java/com/example/domain/repository/MusicRepository.kt", "w") as f:
    f.write(text)
