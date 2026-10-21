package com.example.data.player

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.local.dao.SongDao
import com.example.data.local.entity.SongEntity
import com.example.domain.model.DownloadStatus
import com.example.domain.model.DownloadTask
import com.example.domain.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var queueJob: Job? = null

    private val _downloadTasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadTasks: StateFlow<List<DownloadTask>> = _downloadTasks.asStateFlow()

    var streamResolver: (suspend (Song) -> Song)? = null

    @Volatile
    private var activeCall: okhttp3.Call? = null
    @Volatile
    private var activeSongId: String? = null

    private val downloadsDir: File
        get() {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(context.filesDir, "downloads")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Enqueues a single song or multiple songs into the downloading queue.
     * All items appear immediately in downloadTasks for instant UI population.
     */
    fun enqueueDownloads(songs: List<Song>, sourceCollection: String? = null) {
        if (songs.isEmpty()) return

        val newTasks = songs.mapNotNull { song ->
            if (isSongDownloadedLocally(song.id)) null
            else DownloadTask(
                song = song,
                progress = 0f,
                status = DownloadStatus.QUEUED,
                sourceCollection = sourceCollection
            )
        }
        if (newTasks.isEmpty()) return

        _downloadTasks.update { current ->
            val currentIds = current.map { it.song.id }.toSet()
            current + newTasks.filter { it.song.id !in currentIds }
        }

        startQueueProcessing()
    }

    /**
     * Cancels an active or queued download.
     */
    fun cancelDownload(songId: String) {
        if (activeSongId == songId) {
            try {
                activeCall?.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling call for $songId", e)
            }
        }
        removeTaskFromQueue(songId)
        val safeId = songId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        try {
            File(downloadsDir, "$safeId.tmp").delete()
        } catch (_: Exception) {}
    }

    private fun removeTaskFromQueue(songId: String) {
        _downloadTasks.update { list -> list.filter { it.song.id != songId } }
    }

    private fun startQueueProcessing() {
        synchronized(this) {
            if (queueJob?.isActive == true) return
            queueJob = downloadScope.launch {
                processQueueLoop()
            }
        }
    }

    private suspend fun processQueueLoop() {
        while (true) {
            val nextTask = _downloadTasks.value.firstOrNull { it.status == DownloadStatus.QUEUED } ?: break
            val songId = nextTask.song.id

            _downloadTasks.update { list ->
                list.map { task ->
                    if (task.song.id == songId) task.copy(status = DownloadStatus.DOWNLOADING)
                    else task
                }
            }

            executeSingleDownload(nextTask)
        }
    }

    private suspend fun executeSingleDownload(task: DownloadTask) {
        val song = task.song
        activeSongId = song.id

        try {
            var resolvedSong = song
            var streamUrl = resolvedSong.getStreamUrl(preferHighQuality = true)
            if (streamUrl.isBlank()) {
                val resolved = streamResolver?.invoke(resolvedSong)
                if (resolved != null) {
                    resolvedSong = resolved
                    streamUrl = resolvedSong.getStreamUrl(preferHighQuality = true)
                }
            }

            if (streamUrl.isBlank()) {
                Log.e(TAG, "No stream URL found for ${song.title}")
                markTaskFailed(song.id, "No stream URL available")
                return
            }

            val safeId = song.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val targetFile = File(downloadsDir, "$safeId.mp3")
            val tempFile = File(downloadsDir, "$safeId.tmp")

            if (targetFile.exists() && targetFile.length() > 1024L) {
                ensureSongInDao(resolvedSong, targetFile.absolutePath)
                songDao.updateDownloaded(song.id, true, targetFile.absolutePath)
                markTaskCompleted(song.id)
                return
            }

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

            val call = okHttpClient.newCall(requestBuilder.build())
            activeCall = call

            val response = call.execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    Log.e(TAG, "Download failed with HTTP ${resp.code} for ${song.title}")
                    markTaskFailed(song.id, "HTTP ${resp.code}")
                    return
                }

                val body = resp.body ?: run {
                    markTaskFailed(song.id, "Empty response body")
                    return
                }

                val contentLength = body.contentLength()
                if (tempFile.exists()) tempFile.delete()

                body.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var bytesCopied = 0L
                        var bytes = input.read(buffer)
                        var lastEmit = 0L

                        while (bytes >= 0) {
                            output.write(buffer, 0, bytes)
                            bytesCopied += bytes

                            val now = System.currentTimeMillis()
                            if (now - lastEmit > 90 || bytesCopied == contentLength) {
                                lastEmit = now
                                val progress = if (contentLength > 0L) (bytesCopied.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f) else 0f
                                updateTaskProgress(song.id, progress, bytesCopied, contentLength)
                            }
                            bytes = input.read(buffer)
                        }
                    }
                }
            }

            if (!tempFile.exists() || tempFile.length() <= 0L) {
                tempFile.delete()
                markTaskFailed(song.id, "Downloaded file empty")
                return
            }

            if (targetFile.exists()) targetFile.delete()
            val renamed = tempFile.renameTo(targetFile)
            val finalFile = if (renamed) targetFile else tempFile
            val finalPath = finalFile.absolutePath

            ensureSongInDao(resolvedSong, finalPath)
            songDao.updateDownloaded(song.id, true, finalPath)

            Log.d(TAG, "Successfully downloaded ${song.title} to $finalPath (${finalFile.length()} bytes)")
            markTaskCompleted(song.id)
        } catch (e: Exception) {
            if (e is IOException && (e.message?.contains("Canceled") == true || callIsCanceled(e))) {
                removeTaskFromQueue(song.id)
            } else {
                Log.e(TAG, "Exception downloading track ${song.title}", e)
                markTaskFailed(song.id, e.message ?: "Download error")
            }
        } finally {
            activeCall = null
            activeSongId = null
        }
    }

    private fun callIsCanceled(e: Exception): Boolean {
        return e.cause is java.io.InterruptedIOException || e.message?.contains("Socket closed") == true
    }

    private fun updateTaskProgress(songId: String, progress: Float, bytesCopied: Long, totalBytes: Long) {
        _downloadTasks.update { list ->
            list.map { task ->
                if (task.song.id == songId) {
                    task.copy(
                        progress = progress,
                        bytesDownloaded = bytesCopied,
                        totalBytes = if (totalBytes > 0L) totalBytes else task.totalBytes,
                        status = DownloadStatus.DOWNLOADING
                    )
                } else task
            }
        }
    }

    private suspend fun markTaskCompleted(songId: String) {
        _downloadTasks.update { list ->
            list.map { task ->
                if (task.song.id == songId) task.copy(progress = 1.0f, status = DownloadStatus.COMPLETED)
                else task
            }
        }
        delay(350)
        _downloadTasks.update { list -> list.filter { it.song.id != songId } }
    }

    private fun markTaskFailed(songId: String, error: String) {
        _downloadTasks.update { list ->
            list.map { task ->
                if (task.song.id == songId) task.copy(status = DownloadStatus.FAILED, error = error)
                else task
            }
        }
    }

    /**
     * Downloads a track and awaits completion, returning the local file path.
     * Enqueues the track so it immediately displays in the offline downloads UI.
     */
    suspend fun downloadSong(song: Song): Result<String> = withContext(Dispatchers.IO) {
        val existingFile = getDownloadedFile(song.id)
        if (existingFile != null && existingFile.exists() && existingFile.length() > 1024L) {
            ensureSongInDao(song, existingFile.absolutePath)
            songDao.updateDownloaded(song.id, true, existingFile.absolutePath)
            return@withContext Result.success(existingFile.absolutePath)
        }

        enqueueDownloads(listOf(song))

        // Wait until task is completed or failed
        downloadTasks.map { tasks -> tasks.find { it.song.id == song.id } }
            .filter { it == null || it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.FAILED }
            .first()

        val file = getDownloadedFile(song.id)
        if (file != null && file.exists() && file.length() > 0L) {
            Result.success(file.absolutePath)
        } else {
            Result.failure(IOException("Download failed for ${song.title}"))
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
                    duration = song.duration,
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
            cancelDownload(songId)

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
        if (file.exists() && file.length() > 1024L) return true
        val raw = File(downloadsDir, "$songId.mp3")
        return raw.exists() && raw.length() > 1024L
    }

    fun getDownloadedFile(songId: String): File? {
        val safeId = songId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(downloadsDir, "$safeId.mp3")
        if (file.exists() && file.length() > 1024L) return file
        val raw = File(downloadsDir, "$songId.mp3")
        if (raw.exists() && raw.length() > 1024L) return raw
        return null
    }

    suspend fun syncDownloadedTracks() = withContext(Dispatchers.IO) {
        try {
            val inDb = songDao.getDownloadedSongsSync()
            for (item in inDb) {
                val path = item.localFilePath
                val valid = path != null && File(path).let { it.exists() && it.length() > 1024L }
                if (!valid) {
                    songDao.updateDownloaded(item.id, false, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing downloaded tracks", e)
        }
    }
}
