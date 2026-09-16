package com.example.data.remote.dto

import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DownloadUrlItemDto(
    @Json(name = "quality") val quality: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "link") val link: String? = null
)

@JsonClass(generateAdapter = true)
data class ImageItemDto(
    @Json(name = "quality") val quality: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "link") val link: String? = null
)

@JsonClass(generateAdapter = true)
data class ArtistMiniDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "image") val image: Any? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class SongDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "artist") val artist: String? = null,
    @Json(name = "artists") val artists: Any? = null,
    @Json(name = "primaryArtists") val primaryArtists: Any? = null,
    @Json(name = "album") val album: Any? = null,
    @Json(name = "duration") val duration: Any? = null,
    @Json(name = "duration_sec") val durationSec: Int? = null,
    @Json(name = "image") val image: Any? = null,
    @Json(name = "artwork") val artwork: String? = null,
    @Json(name = "cover_url") val coverUrl: String? = null,
    @Json(name = "downloadUrl") val downloadUrl: Any? = null,
    @Json(name = "stream_160") val stream160: String? = null,
    @Json(name = "stream_320") val stream320: String? = null,
    @Json(name = "stream_url") val streamUrl: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "lyrics") val lyrics: String? = null,
    @Json(name = "year") val year: Any? = null,
    @Json(name = "label") val label: String? = null,
    @Json(name = "language") val language: String? = null
) {
    fun toDomain(): Song {
        val songId = id ?: ((title ?: name ?: "song") + "_" + System.currentTimeMillis())
        val songTitle = title ?: name ?: "Unknown Track"
        
        val songArtist = parseArtistString(primaryArtists, artists, artist)
        val (albumName, albumId) = parseAlbumInfo(album)
        
        val durSec: Int = durationSec ?: when (duration) {
            is Number -> duration.toInt()
            is String -> duration.toIntOrNull() ?: 210
            else -> 210
        }
        
        val artUrl = parseImageUrl(image, artwork, coverUrl, songId)
        val downloadMap = parseDownloadUrls(downloadUrl, stream160, stream320, streamUrl)
        
        val s160 = downloadMap["160kbps"] ?: stream160 ?: downloadMap["160"] ?: downloadMap["96kbps"] ?: downloadMap["48kbps"] ?: ""
        val s320 = downloadMap["320kbps"] ?: stream320 ?: downloadMap["320"] ?: streamUrl ?: s160
        
        val yearStr = when (year) {
            is String -> year
            is Number -> year.toInt().toString()
            else -> ""
        }

        return Song(
            id = songId,
            title = songTitle,
            artist = songArtist,
            album = albumName,
            durationSec = durSec,
            artworkUrl = artUrl,
            stream160Url = s160,
            stream320Url = s320,
            lyrics = lyrics,
            year = yearStr,
            albumId = albumId,
            downloadUrls = downloadMap
        )
    }

    companion object {
        fun parseArtistString(primaryArtists: Any?, artists: Any?, fallbackArtist: String?): String {
            if (primaryArtists is String && primaryArtists.isNotBlank()) return primaryArtists
            if (artists is String && artists.isNotBlank()) return artists
            if (fallbackArtist?.isNotBlank() == true) return fallbackArtist

            if (artists is Map<*, *>) {
                val primary = artists["primary"]
                if (primary is List<*>) {
                    val names = primary.mapNotNull { item ->
                        when (item) {
                            is Map<*, *> -> item["name"] as? String
                            is String -> item
                            else -> null
                        }
                    }
                    if (names.isNotEmpty()) return names.joinToString(", ")
                }
            }

            if (primaryArtists is List<*>) {
                val names = primaryArtists.mapNotNull { item ->
                    when (item) {
                        is Map<*, *> -> item["name"] as? String
                        is String -> item
                        else -> null
                    }
                }
                if (names.isNotEmpty()) return names.joinToString(", ")
            }

            return "Unknown Artist"
        }

        fun parseAlbumInfo(album: Any?): Pair<String, String> {
            return when (album) {
                is String -> album to ""
                is Map<*, *> -> {
                    val name = album["name"] as? String ?: album["title"] as? String ?: "Single"
                    val id = album["id"] as? String ?: ""
                    name to id
                }
                else -> "Single" to ""
            }
        }

        fun parseImageUrl(image: Any?, artwork: String?, coverUrl: String?, fallbackId: String): String {
            if (image is String && image.isNotBlank()) return image
            if (artwork?.isNotBlank() == true) return artwork
            if (coverUrl?.isNotBlank() == true) return coverUrl

            if (image is List<*>) {
                // Find highest quality e.g. 500x500
                var highestUrl: String? = null
                for (item in image) {
                    if (item is Map<*, *>) {
                        val link = item["url"] as? String ?: item["link"] as? String
                        val quality = item["quality"] as? String
                        if (link?.isNotBlank() == true) {
                            if (quality == "500x500" || quality?.contains("500") == true) {
                                return link
                            }
                            highestUrl = link
                        }
                    } else if (item is String && item.isNotBlank()) {
                        highestUrl = item
                    }
                }
                if (highestUrl != null) return highestUrl
            }

            return "https://picsum.photos/seed/$fallbackId/400/400"
        }

        fun parseDownloadUrls(
            downloadUrl: Any?,
            stream160: String?,
            stream320: String?,
            streamUrl: String? = null
        ): Map<String, String> {
            val map = mutableMapOf<String, String>()

            fun isValidMediaUrl(link: String?): Boolean {
                if (link.isNullOrBlank()) return false
                if (link.contains("jiosaavn.com/song") || link.contains("jiosaavn.com/album") || link.endsWith(".html")) return false
                return link.startsWith("http://") || link.startsWith("https://")
            }

            if (isValidMediaUrl(stream160)) map["160kbps"] = stream160!!
            if (isValidMediaUrl(stream320)) map["320kbps"] = stream320!!
            if (isValidMediaUrl(streamUrl)) map["standard"] = streamUrl!!

            if (downloadUrl is List<*>) {
                for (item in downloadUrl) {
                    if (item is Map<*, *>) {
                        val quality = item["quality"] as? String ?: ""
                        val link = item["url"] as? String ?: item["link"] as? String
                        if (isValidMediaUrl(link)) {
                            map[quality] = link!!
                            when {
                                quality.contains("320") -> map["320kbps"] = link
                                quality.contains("160") -> map["160kbps"] = link
                                quality.contains("96") -> map["96kbps"] = link
                                quality.contains("48") -> map["48kbps"] = link
                                quality.contains("12") -> map["12kbps"] = link
                            }
                        }
                    } else if (item is String && isValidMediaUrl(item)) {
                        map["320kbps"] = item
                    }
                }
            } else if (downloadUrl is String && isValidMediaUrl(downloadUrl)) {
                map["320kbps"] = downloadUrl
            }

            return map
        }
    }
}

@JsonClass(generateAdapter = true)
data class SearchResponseDto(
    @Json(name = "success") val success: Boolean? = true,
    @Json(name = "songs") val songs: List<SongDto>? = null,
    @Json(name = "results") val results: List<SongDto>? = null,
    @Json(name = "data") val data: Any? = null,
    @Json(name = "albums") val albums: List<AlbumDto>? = null,
    @Json(name = "playlists") val playlists: List<PlaylistDto>? = null,
    @Json(name = "artists") val artists: List<ArtistDto>? = null
)

@JsonClass(generateAdapter = true)
data class AlbumDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "artist") val artist: String? = null,
    @Json(name = "artists") val artists: Any? = null,
    @Json(name = "primaryArtists") val primaryArtists: Any? = null,
    @Json(name = "image") val image: Any? = null,
    @Json(name = "year") val year: Any? = null,
    @Json(name = "songCount") val songCount: Any? = null,
    @Json(name = "songs") val songs: List<SongDto>? = null
) {
    fun toDomain(): Album {
        val albumId = id ?: "album_${title ?: name ?: System.currentTimeMillis()}"
        val albumTitle = title ?: name ?: "Featured Album"
        val albumArtist = SongDto.parseArtistString(primaryArtists, artists, artist)
        val art = SongDto.parseImageUrl(image, null, null, albumId)
        val songList = songs?.map { it.toDomain() } ?: emptyList()
        val yearStr = when (year) {
            is String -> year
            is Number -> year.toInt().toString()
            else -> "2024"
        }
        val count = when (songCount) {
            is Number -> songCount.toInt()
            is String -> songCount.toIntOrNull() ?: songList.size
            else -> songList.size
        }

        return Album(
            id = albumId,
            title = albumTitle,
            artist = albumArtist,
            artworkUrl = art,
            year = yearStr,
            trackCount = if (count > 0) count else songList.size,
            songs = songList
        )
    }
}

@JsonClass(generateAdapter = true)
data class PlaylistDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "image") val image: Any? = null,
    @Json(name = "songCount") val songCount: Any? = null,
    @Json(name = "firstname") val firstname: String? = null,
    @Json(name = "songs") val songs: List<SongDto>? = null
) {
    fun toDomain(): Playlist {
        val pId = id ?: "playlist_${title ?: name ?: System.currentTimeMillis()}"
        val pTitle = title ?: name ?: "Curated Playlist"
        val desc = description ?: "Curated music selection"
        val art = SongDto.parseImageUrl(image, null, null, pId)
        val songList = songs?.map { it.toDomain() } ?: emptyList()
        val count = when (songCount) {
            is Number -> songCount.toInt()
            is String -> songCount.toIntOrNull() ?: songList.size
            else -> songList.size
        }

        return Playlist(
            id = pId,
            title = pTitle,
            description = desc,
            artworkUrl = art,
            trackCount = if (count > 0) count else songList.size,
            creator = firstname ?: "Pulse Music",
            songs = songList
        )
    }
}

@JsonClass(generateAdapter = true)
data class ArtistDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "image") val image: Any? = null,
    @Json(name = "bio") val bio: Any? = null,
    @Json(name = "fanCount") val fanCount: Any? = null,
    @Json(name = "followerCount") val followerCount: Any? = null,
    @Json(name = "topSongs") val topSongs: List<SongDto>? = null,
    @Json(name = "topAlbums") val topAlbums: List<AlbumDto>? = null,
    @Json(name = "singles") val singles: List<SongDto>? = null,
    @Json(name = "songs") val songs: List<SongDto>? = null,
    @Json(name = "albums") val albums: List<AlbumDto>? = null
) {
    fun toDomain(): Artist {
        val artistId = id ?: "artist_${name ?: title ?: System.currentTimeMillis()}"
        val artistName = name ?: title ?: "Artist"
        val art = SongDto.parseImageUrl(image, null, null, artistId)
        
        val bioStr = when (bio) {
            is String -> bio
            is List<*> -> {
                bio.mapNotNull { item ->
                    if (item is Map<*, *>) item["text"] as? String ?: item["title"] as? String else item?.toString()
                }.joinToString("\n\n")
            }
            else -> "Chart-topping artist with millions of monthly listeners worldwide."
        }

        val followers = when (val f = followerCount ?: fanCount) {
            is Number -> "${f.toInt() / 1000}K Followers"
            is String -> if (f.isNotBlank()) "$f Followers" else "Popular Artist"
            else -> "Top Verified Artist"
        }

        val allSongs = (topSongs ?: songs ?: singles ?: emptyList()).map { it.toDomain() }
        val allAlbums = (topAlbums ?: albums ?: emptyList()).map { it.toDomain() }

        return Artist(
            id = artistId,
            name = artistName,
            imageUrl = art,
            bio = if (bioStr.isNotBlank()) bioStr else "Popular recording artist and performer.",
            followerCount = followers,
            topSongs = allSongs,
            topAlbums = allAlbums
        )
    }
}

@JsonClass(generateAdapter = true)
data class LyricsDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "lyrics") val lyrics: String? = null,
    @Json(name = "synced_lyrics") val syncedLyrics: String? = null
)

@JsonClass(generateAdapter = true)
data class RecommendationsDto(
    @Json(name = "trending") val trending: List<SongDto>? = null,
    @Json(name = "featured") val featured: List<SongDto>? = null,
    @Json(name = "albums") val albums: List<AlbumDto>? = null,
    @Json(name = "playlists") val playlists: List<PlaylistDto>? = null
)
