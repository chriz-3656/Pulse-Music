import re

with open("app/src/main/java/com/example/data/repository/MusicRepositoryImpl.kt", "r") as f:
    text = f.read()

old_func = """    override suspend fun getPlaylistDetails(id: String): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val playlist = api.LoadPlaylist.loadPlaylist(id).getOrNull()"""

new_func = """    override suspend fun getPlaylistDetails(id: String): Result<Playlist> = withContext(Dispatchers.IO) {
        val localPlaylist = playlistDao.getPlaylistById(id)
        if (localPlaylist != null) {
            val localSongs = playlistDao.getSongsForPlaylistSync(id)
            val songs = localSongs.map { entity -> Song(entity.id, entity.title, entity.artist, entity.album, entity.durationSec, entity.artworkUrl, entity.stream160Url, entity.stream320Url, entity.lyrics, entity.isDownloaded, entity.isFavorite, entity.localFilePath, entity.year, entity.artistId, entity.albumId) }
            val playlist = Playlist(localPlaylist.id, localPlaylist.title, localPlaylist.description, localPlaylist.artworkUrl, songs.size, localPlaylist.creator, localPlaylist.createdAt, songs)
            return@withContext Result.success(playlist)
        }

        try {
            val playlist = api.LoadPlaylist.loadPlaylist(id).getOrNull()"""

text = text.replace(old_func, new_func)

with open("app/src/main/java/com/example/data/repository/MusicRepositoryImpl.kt", "w") as f:
    f.write(text)
