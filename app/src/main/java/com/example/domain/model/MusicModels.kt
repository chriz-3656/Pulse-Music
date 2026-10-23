package com.example.domain.model

data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0L,
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
    val downloadUrls: Map<String, String> = emptyMap(),
    val sourceProvider: String = ""
) {
    val resolvedProvider: MusicProvider
        get() {
            if (sourceProvider.isNotBlank()) {
                val p = MusicProvider.fromId(sourceProvider)
                if (p != MusicProvider.AUTO) return p
            }
            if (id.startsWith("sc_") || artworkUrl.contains("sndcdn.com") || artworkUrl.contains("soundcloud.com") || stream160Url.contains("sndcdn.com")) {
                return MusicProvider.SOUNDCLOUD
            }
            if (artworkUrl.contains("saavncdn.com") || artworkUrl.contains("jiosaavn.com") || stream320Url.contains("saavn") || stream160Url.contains("saavn")) {
                return MusicProvider.JIOSAAVN
            }
            if (artworkUrl.contains("ytimg.com") || artworkUrl.contains("googleusercontent.com") || artworkUrl.contains("ggpht.com") || stream160Url.contains("googlevideo.com")) {
                return MusicProvider.YOUTUBE
            }
            if (id.length == 11 && !id.startsWith("sc_") && id.matches(Regex("^[A-Za-z0-9_-]{11}$"))) {
                return MusicProvider.YOUTUBE
            }
            if (id.isNotBlank() && (id.all { it.isDigit() } || id.length in 6..9)) {
                return MusicProvider.JIOSAAVN
            }
            return MusicProvider.AUTO
        }

    val streamUrl: String
        get() = getStreamUrl(preferHighQuality = true)

    fun getStreamUrl(preferHighQuality: Boolean): String {
        if (!localFilePath.isNullOrBlank()) {
            val file = java.io.File(localFilePath)
            if (file.exists() && file.length() > 0) {
                return file.toURI().toString()
            }
        }

        fun secureMediaUrl(url: String?): String? {
            if (url.isNullOrBlank()) return null
            if (url.contains("jiosaavn.com/song") || url.contains("jiosaavn.com/album") || url.endsWith(".html")) return null
            val secure = if (url.startsWith("http://")) {
                "https://" + url.removePrefix("http://")
            } else {
                url
            }
            return secure.takeIf {
                it.startsWith("https://") || it.startsWith("file:") || it.startsWith("content://")
            }
        }

        val qualityOrder = if (preferHighQuality) {
            listOf("320kbps", "320", "160kbps", "160", "96kbps", "96", "48kbps", "48", "12kbps", "12", "standard", "default")
        } else {
            listOf("160kbps", "160", "96kbps", "96", "48kbps", "48", "12kbps", "12", "standard", "default")
        }

        for (q in qualityOrder) {
            secureMediaUrl(downloadUrls[q])?.let { return it }
        }

        if (preferHighQuality) {
            secureMediaUrl(stream320Url)?.let { return it }
            secureMediaUrl(stream160Url)?.let { return it }
        } else {
            secureMediaUrl(stream160Url)?.let { return it }
            secureMediaUrl(stream320Url)?.let { return it }
        }

        for (v in downloadUrls.values) {
            secureMediaUrl(v)?.let { return it }
        }

        return ""
    }

    val formattedDuration: String
        get() {
            val minutes = (duration / 1000) / 60
            val seconds = (duration / 1000) % 60
            if (duration <= 0L) return "--:--"
            return String.format("%d:%02d", minutes, seconds)
        }
}

data class Album(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val artistId: String = "",
    val artworkUrl: String = "",
    val year: String = "",
    val trackCount: Int = 0,
    val songs: List<Song> = emptyList()
)

data class Playlist(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val artworkUrl: String = "",
    val trackCount: Int = 0,
    val creator: String = "Pulse Music",
    val createdAt: Long = System.currentTimeMillis(),
    val songs: List<Song> = emptyList()
)

data class Artist(
    val id: String = "",
    val name: String = "",
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
    val id: String = "",
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
    val apiProvider: String = "youtube",
    val isPureOfflineMode: Boolean = false
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

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class DownloadTask(
    val song: Song,
    val progress: Float = 0f,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val sourceCollection: String? = null,
    val error: String? = null
)
