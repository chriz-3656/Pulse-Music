package com.example.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationSec: Int = 0,
    val artworkUrl: String = "",
    val stream160Url: String = "",
    val stream320Url: String = "",
    val lyrics: String? = null,
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val localFilePath: String? = null,
    val year: String = "",
    val artistId: String = "",
    val albumId: String = "",
    val downloadUrls: Map<String, String> = emptyMap()
) {
    val streamUrl: String
        get() = getStreamUrl(preferHighQuality = true)

    fun getStreamUrl(preferHighQuality: Boolean): String {
        if (!localFilePath.isNullOrBlank()) {
            val file = java.io.File(localFilePath)
            if (file.exists() && file.length() > 0) {
                return file.toURI().toString()
            }
        }

        fun isValidMediaUrl(url: String?): Boolean {
            if (url.isNullOrBlank()) return false
            if (url.contains("jiosaavn.com/song") || url.contains("jiosaavn.com/album") || url.endsWith(".html")) return false
            return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file:") || url.startsWith("content://")
        }

        val qualityOrder = if (preferHighQuality) {
            listOf("320kbps", "320", "160kbps", "160", "96kbps", "96", "48kbps", "48", "12kbps", "12", "standard", "default")
        } else {
            listOf("160kbps", "160", "96kbps", "96", "48kbps", "48", "12kbps", "12", "standard", "default")
        }

        for (q in qualityOrder) {
            val candidate = downloadUrls[q]
            if (isValidMediaUrl(candidate)) {
                return candidate!!
            }
        }

        if (preferHighQuality) {
            if (isValidMediaUrl(stream320Url)) return stream320Url
            if (isValidMediaUrl(stream160Url)) return stream160Url
        } else {
            if (isValidMediaUrl(stream160Url)) return stream160Url
            if (isValidMediaUrl(stream320Url)) return stream320Url
        }

        for (v in downloadUrls.values) {
            if (isValidMediaUrl(v)) return v
        }

        return ""
    }

    val formattedDuration: String
        get() {
            val minutes = durationSec / 60
            val seconds = durationSec % 60
            return String.format("%d:%02d", minutes, seconds)
        }
}

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val artworkUrl: String = "",
    val year: String = "",
    val trackCount: Int = 0,
    val songs: List<Song> = emptyList()
)

data class Playlist(
    val id: String,
    val title: String,
    val description: String = "",
    val artworkUrl: String = "",
    val trackCount: Int = 0,
    val creator: String = "Pulse Music",
    val createdAt: Long = System.currentTimeMillis(),
    val songs: List<Song> = emptyList()
)

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String = "",
    val bio: String = "",
    val followerCount: String = "",
    val topSongs: List<Song> = emptyList(),
    val topAlbums: List<Album> = emptyList()
)

enum class RepeatMode {
    OFF, ONE, ALL
}

enum class AudioQuality(val label: String, val bitrate: String) {
    STANDARD("Standard", "160 kbps"),
    HIGH("High Quality", "320 kbps")
}

enum class MusicProvider(
    val id: String,
    val displayName: String,
    val description: String,
    val badge: String,
    val maxBitrate: String,
    val format: String
) {
    AUTO("auto", "Auto (Multi-Source)", "Intelligent routing across all sources with automatic failover", "OPTIMAL", "320 kbps", "Dynamic"),
    JIOSAAVN("jiosaavn", "JioSaavn CDN", "High-fidelity 320 kbps direct audio streams & global discography", "HQ 320K", "320 kbps", "MP4 / AAC"),
    SOUNDCLOUD("soundcloud", "SoundCloud", "Direct progressive streams for EDM, indie, and creator mixes", "WEB DIRECT", "160 kbps", "MP3 / AAC"),
    YOUTUBE("youtube", "YouTube Music", "Comprehensive YouTube Music catalog, charts, & radio mixes", "UNIVERSAL", "256 kbps", "M4A / OPUS");

    companion object {
        fun fromId(id: String): MusicProvider =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: AUTO
    }
}

data class ProviderStatus(
    val provider: MusicProvider,
    val isOnline: Boolean = true,
    val latencyMs: Long = 0,
    val statusMessage: String = "Online",
    val isOperational: Boolean = isOnline,
    val details: String = statusMessage
)

data class UserSettings(
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val cacheSizeLimitMb: Int = 1000,
    val crossfadeDurationSec: Int = 2,
    val isDarkTheme: Boolean = true,
    val autoSkipFailedTracks: Boolean = true,
    val autoplayEnabled: Boolean = true,
    val customApiBaseUrl: String = "",
    val apiProvider: String = "youtube"
) {
    val provider: MusicProvider get() = MusicProvider.fromId(apiProvider)
}

data class ImportProgress(
    val progress: Float,
    val message: String,
    val isComplete: Boolean = false,
    val playlistId: String? = null,
    val error: String? = null
)
