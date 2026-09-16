package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.Playlist
import com.example.domain.model.Song

@Entity(tableName = "cached_songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val artworkUrl: String,
    val stream160Url: String,
    val stream320Url: String,
    val lyrics: String?,
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val localFilePath: String? = null,
    val year: String = "",
    val artistId: String = "",
    val albumId: String = "",
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationSec = durationSec,
            artworkUrl = artworkUrl,
            stream160Url = stream160Url,
            stream320Url = stream320Url,
            lyrics = lyrics,
            isDownloaded = isDownloaded,
            isFavorite = isFavorite,
            localFilePath = localFilePath,
            year = year,
            artistId = artistId,
            albumId = albumId
        )
    }

    companion object {
        fun fromDomain(song: Song, cachedAt: Long = System.currentTimeMillis()): SongEntity {
            return SongEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationSec = song.durationSec,
                artworkUrl = song.artworkUrl,
                stream160Url = song.stream160Url,
                stream320Url = song.stream320Url,
                lyrics = song.lyrics,
                isDownloaded = song.isDownloaded,
                isFavorite = song.isFavorite,
                localFilePath = song.localFilePath,
                year = song.year,
                artistId = song.artistId,
                albumId = song.albumId,
                cachedAt = cachedAt
            )
        }
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val artworkUrl: String,
    val creator: String = "Pulse Music",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(songs: List<Song> = emptyList()): Playlist {
        return Playlist(
            id = id,
            title = title,
            description = description,
            artworkUrl = artworkUrl,
            trackCount = songs.size,
            creator = creator,
            createdAt = createdAt,
            songs = songs
        )
    }
}

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val orderIndex: Int
)

@Entity(tableName = "cached_albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val artworkUrl: String,
    val year: String,
    val trackCount: Int,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(songs: List<Song> = emptyList()): Album {
        return Album(
            id = id,
            title = title,
            artist = artist,
            artistId = artistId,
            artworkUrl = artworkUrl,
            year = year,
            trackCount = if (trackCount > 0) trackCount else songs.size,
            songs = songs
        )
    }

    companion object {
        fun fromDomain(album: Album): AlbumEntity {
            return AlbumEntity(
                id = album.id,
                title = album.title,
                artist = album.artist,
                artistId = album.artistId,
                artworkUrl = album.artworkUrl,
                year = album.year,
                trackCount = album.trackCount
            )
        }
    }
}

@Entity(
    tableName = "album_songs",
    primaryKeys = ["albumId", "songId"]
)
data class AlbumSongCrossRef(
    val albumId: String,
    val songId: String,
    val orderIndex: Int
)

@Entity(tableName = "cached_artists")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String,
    val bio: String,
    val followerCount: String,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(topSongs: List<Song> = emptyList(), albums: List<Album> = emptyList()): Artist {
        return Artist(
            id = id,
            name = name,
            imageUrl = imageUrl,
            bio = bio,
            followerCount = followerCount,
            topSongs = topSongs,
            topAlbums = albums
        )
    }

    companion object {
        fun fromDomain(artist: Artist): ArtistEntity {
            return ArtistEntity(
                id = artist.id,
                name = artist.name,
                imageUrl = artist.imageUrl,
                bio = artist.bio,
                followerCount = artist.followerCount
            )
        }
    }
}

@Entity(
    tableName = "artist_songs",
    primaryKeys = ["artistId", "songId"]
)
data class ArtistSongCrossRef(
    val artistId: String,
    val songId: String,
    val orderIndex: Int
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
