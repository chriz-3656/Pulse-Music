import re

with open("app/src/main/java/com/example/data/repository/MusicRepositoryImpl.kt", "r") as f:
    text = f.read()

old_loop = """                try {
                    val searchResults = searchAll(query)
                    val bestMatch = searchResults.songs.firstOrNull()
                    
                    if (bestMatch != null) {
                        addSongToPlaylist(dbPlaylistId, bestMatch)
                        matchedTracks++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }"""

new_loop = """                try {
                    val searchResults = searchSongs(query)
                    val bestMatch = searchResults.firstOrNull()
                    
                    if (bestMatch != null) {
                        songDao.insertSong(com.example.data.local.entity.SongEntity(bestMatch.id, bestMatch.title, bestMatch.artist, bestMatch.album, bestMatch.durationSec, bestMatch.artworkUrl, bestMatch.stream160Url, bestMatch.stream320Url, bestMatch.lyrics, bestMatch.isDownloaded, bestMatch.isFavorite, bestMatch.localFilePath, bestMatch.year, bestMatch.artistId, bestMatch.albumId, System.currentTimeMillis()))
                        playlistDao.insertPlaylistSong(com.example.data.local.entity.PlaylistSongCrossRef(dbPlaylistId, bestMatch.id, i))
                        matchedTracks++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                kotlinx.coroutines.delay(1000)"""

text = text.replace(old_loop, new_loop)

with open("app/src/main/java/com/example/data/repository/MusicRepositoryImpl.kt", "w") as f:
    f.write(text)
