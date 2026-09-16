package com.example.data.player

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.local.dao.SongDao
import com.example.data.local.entity.SongEntity
import com.example.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TrackDownloadManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val songDao: SongDao
) {
    companion object {
        private const val TAG = "TrackDownloadManager"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    private val downloadsDir: File
        get() {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(context.filesDir, "downloads")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Downloads a track stream directly to local storage using OkHttp streaming.
     * Guarantees headers (SoundCloud/JioSaavn), atomic writing, and local database sync.
     */
    suspend fun downloadSong(song: Song): Result<String> = withContext(Dispatchers.IO) {
        try {
            val streamUrl = song.getStreamUrl(preferHighQuality = true)
            if (streamUrl.isBlank()) {
                Log.e(TAG, "No stream URL available for download: ${song.title}")
                return@withContext Result.failure(IllegalArgumentException("No stream URL available for ${song.title}"))
            }

            val safeId = song.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetFile = File(downloadsDir, "$safeId.mp3")
            val tempFile = File(downloadsDir, "$safeId.tmp")

            // If already downloaded and valid, update DB and return
            if (targetFile.exists() && targetFile.length() > 1024L) {
                ensureSongInDao(song, targetFile.absolutePath)
                songDao.updateDownloaded(song.id, true, targetFile.absolutePath)
                Log.d(TAG, "Song already downloaded: ${song.title} at ${targetFile.absolutePath}")
                return@withContext Result.success(targetFile.absolutePath)
            }

            Log.d(TAG, "Starting download for ${song.title} from: $streamUrl")
            val requestBuilder = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")

            if (streamUrl.contains("sndcdn") || song.id.startsWith("sc_")) {
                requestBuilder.header("Referer", "https://soundcloud.com/")
                requestBuilder.header("Origin", "https://soundcloud.com")
            } else if (streamUrl.contains("saavn") || streamUrl.contains("jiosaavn")) {
                requestBuilder.header("Referer", "https://www.jiosaavn.com/")
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed with HTTP ${response.code} for ${song.title}")
                return@withContext Result.failure(IOException("HTTP ${response.code} downloading track"))
            }

            val body = response.body ?: return@withContext Result.failure(IOException("Empty response body"))
            
            // Clean up any stale temp file
            if (tempFile.exists()) tempFile.delete()

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }

            if (!tempFile.exists() || tempFile.length() <= 0L) {
                tempFile.delete()
                return@withContext Result.failure(IOException("Downloaded file was empty"))
            }

            // Atomic rename to final file
            if (targetFile.exists()) targetFile.delete()
            val renamed = tempFile.renameTo(targetFile)
            val finalFile = if (renamed) targetFile else tempFile
            val finalPath = finalFile.absolutePath

            ensureSongInDao(song, finalPath)
            songDao.updateDownloaded(song.id, true, finalPath)
            Log.d(TAG, "Successfully downloaded ${song.title} to $finalPath (${finalFile.length()} bytes)")
            Result.success(finalPath)
        } catch (e: Exception) {
            Log.e(TAG, "Exception downloading track ${song.title}", e)
            Result.failure(e)
        }
    }

    private suspend fun ensureSongInDao(song: Song, localPath: String) {
        val existing = songDao.getSongById(song.id)
        if (existing != null) {
            songDao.updateDownloaded(song.id, true, localPath)
        } else {
            songDao.insertSong(
                SongEntity(
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    durationSec = song.durationSec,
                    artworkUrl = song.artworkUrl,
                    stream160Url = song.stream160Url,
                    stream320Url = song.stream320Url,
                    lyrics = song.lyrics,
                    isDownloaded = true,
                    isFavorite = song.isFavorite,
                    localFilePath = localPath,
                    year = song.year,
                    artistId = song.artistId,
                    albumId = song.albumId,
                    cachedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun removeDownload(songId: String) = withContext(Dispatchers.IO) {
        try {
            val safeId = songId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetFile = File(downloadsDir, "$safeId.mp3")
            if (targetFile.exists()) targetFile.delete()

            val rawFile = File(downloadsDir, "$songId.mp3")
            if (rawFile.exists()) rawFile.delete()

            songDao.updateDownloaded(songId, false, null)
            Log.d(TAG, "Removed download for song ID $songId")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing download for song ID $songId", e)
        }
    }

    fun isSongDownloadedLocally(songId: String): Boolean {
        val safeId = songId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(downloadsDir, "$safeId.mp3")
        if (file.exists() && file.length() > 0L) return true
        val raw = File(downloadsDir, "$songId.mp3")
        return raw.exists() && raw.length() > 0L
    }

    fun getDownloadedFile(songId: String): File? {
        val safeId = songId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(downloadsDir, "$safeId.mp3")
        if (file.exists() && file.length() > 0L) return file
        val raw = File(downloadsDir, "$songId.mp3")
        if (raw.exists() && raw.length() > 0L) return raw
        return null
    }

    suspend fun syncDownloadedTracks() = withContext(Dispatchers.IO) {
        try {
            val inDb = songDao.getDownloadedSongsSync()
            for (item in inDb) {
                val path = item.localFilePath
                val valid = path != null && File(path).let { it.exists() && it.length() > 0L }
                if (!valid) {
                    songDao.updateDownloaded(item.id, false, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing downloaded tracks", e)
        }
    }
}
