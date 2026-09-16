package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.AlbumEntity
import com.example.data.local.entity.AlbumSongCrossRef
import com.example.data.local.entity.ArtistEntity
import com.example.data.local.entity.ArtistSongCrossRef
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistSongCrossRef
import com.example.data.local.entity.RecentSearchEntity
import com.example.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM cached_songs WHERE id = :id")
    suspend fun getSongById(id: String): SongEntity?

    @Query("SELECT * FROM cached_songs WHERE id IN (:ids)")
    suspend fun getSongsByIds(ids: List<String>): List<SongEntity>

    @Query("SELECT * FROM cached_songs WHERE id = :id")
    fun observeSongById(id: String): Flow<SongEntity?>

    @Query("SELECT * FROM cached_songs WHERE isFavorite = 1 ORDER BY cachedAt DESC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM cached_songs WHERE isDownloaded = 1 ORDER BY cachedAt DESC")
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM cached_songs WHERE isDownloaded = 1 ORDER BY cachedAt DESC")
    suspend fun getDownloadedSongsSync(): List<SongEntity>

    @Query("SELECT * FROM cached_songs ORDER BY cachedAt DESC LIMIT 100")
    fun getAllCachedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM cached_songs ORDER BY cachedAt DESC LIMIT 100")
    suspend fun getAllCachedSongsSync(): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("UPDATE cached_songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE cached_songs SET isDownloaded = :isDownloaded, localFilePath = :localPath WHERE id = :id")
    suspend fun updateDownloaded(id: String, isDownloaded: Boolean, localPath: String?)

    @Query("UPDATE cached_songs SET lyrics = :lyrics WHERE id = :id")
    suspend fun updateLyrics(id: String, lyrics: String)

    @Query("DELETE FROM cached_songs WHERE id = :id")
    suspend fun deleteSong(id: String)

    @Query("DELETE FROM cached_songs WHERE isFavorite = 0 AND isDownloaded = 0 AND cachedAt < :thresholdTime")
    suspend fun purgeOldCache(thresholdTime: Long)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun deletePlaylistSongs(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(crossRef: PlaylistSongCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongs(crossRefs: List<PlaylistSongCrossRef>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Query("""
        SELECT s.* FROM cached_songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.orderIndex ASC
    """)
    fun getSongsForPlaylist(playlistId: String): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM cached_songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.orderIndex ASC
    """)
    suspend fun getSongsForPlaylistSync(playlistId: String): List<SongEntity>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getPlaylistTrackCount(playlistId: String): Int
}

@Dao
interface AlbumDao {
    @Query("SELECT * FROM cached_albums WHERE id = :id")
    suspend fun getAlbumById(id: String): AlbumEntity?

    @Query("SELECT * FROM cached_albums WHERE artistId = :artistId ORDER BY year DESC")
    suspend fun getAlbumsForArtist(artistId: String): List<AlbumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumSongs(crossRefs: List<AlbumSongCrossRef>)

    @Query("""
        SELECT s.* FROM cached_songs s
        INNER JOIN album_songs asongs ON s.id = asongs.songId
        WHERE asongs.albumId = :albumId
        ORDER BY asongs.orderIndex ASC
    """)
    suspend fun getSongsForAlbum(albumId: String): List<SongEntity>
}

@Dao
interface ArtistDao {
    @Query("SELECT * FROM cached_artists WHERE id = :id")
    suspend fun getArtistById(id: String): ArtistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: ArtistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtistSongs(crossRefs: List<ArtistSongCrossRef>)

    @Query("""
        SELECT s.* FROM cached_songs s
        INNER JOIN artist_songs asongs ON s.id = asongs.songId
        WHERE asongs.artistId = :artistId
        ORDER BY asongs.orderIndex ASC
    """)
    suspend fun getSongsForArtist(artistId: String): List<SongEntity>
}

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun deleteSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearAll()
}
