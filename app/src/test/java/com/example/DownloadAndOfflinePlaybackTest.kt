package com.example

import com.example.domain.model.AudioQuality
import com.example.domain.model.Song
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DownloadAndOfflinePlaybackTest {

    @Test
    fun testSongLocalFilePathPrioritizedWhenAvailable() {
        val tempFile = File.createTempFile("offline_track", ".mp3")
        tempFile.writeText("fake mp3 audio bytes for testing")

        try {
            val song = Song(
                id = "test_song_123",
                title = "Offline Masterpiece",
                artist = "Analog Unit",
                album = "Vintage Vault",
                durationSec = 240,
                stream160Url = "https://cdn.example.com/stream160.mp3",
                stream320Url = "https://cdn.example.com/stream320.mp3",
                isDownloaded = true,
                localFilePath = tempFile.absolutePath
            )

            // When local file exists, getStreamUrl must return the local file URI
            val resolvedUrl = song.getStreamUrl(preferHighQuality = true)
            assertEquals(tempFile.toURI().toString(), resolvedUrl)

            // Even when preferHighQuality = false, local file is prioritized
            val lowQualityUrl = song.getStreamUrl(preferHighQuality = false)
            assertEquals(tempFile.toURI().toString(), lowQualityUrl)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testSongFallsBackToOnlineStreamsWhenLocalFileMissing() {
        val nonExistentPath = "/non/existent/path/song.mp3"
        val song = Song(
            id = "test_song_456",
            title = "Online Only Track",
            artist = "Streamer",
            album = "Cloud Tapes",
            durationSec = 180,
            stream160Url = "https://cdn.example.com/160.mp3",
            stream320Url = "https://cdn.example.com/320.mp3",
            isDownloaded = false,
            localFilePath = nonExistentPath
        )

        // Non-existent local file should fallback to online streams
        val highQualityStream = song.getStreamUrl(preferHighQuality = true)
        assertEquals("https://cdn.example.com/320.mp3", highQualityStream)

        val standardStream = song.getStreamUrl(preferHighQuality = false)
        assertEquals("https://cdn.example.com/160.mp3", standardStream)
    }

    @Test
    fun testOfflinePlaybackFileUriResolution() {
        val tempAudio = File.createTempFile("tape_deck_sample", ".mp3")
        tempAudio.writeText("PCM audio sample")

        try {
            val song = Song(
                id = "sc_123456",
                title = "SoundCloud Offline",
                artist = "Sound Artist",
                durationSec = 210,
                isDownloaded = true,
                localFilePath = tempAudio.absolutePath
            )

            // Verify local file resolution logic used in playback service
            var localFile: File? = null
            if (!song.localFilePath.isNullOrBlank()) {
                val f = File(song.localFilePath!!)
                if (f.exists() && f.length() > 0L) {
                    localFile = f
                }
            }

            assertNotNull("Local file should be detected", localFile)
            assertTrue("Local file must exist", localFile!!.exists())
            assertTrue("Local file must have bytes", localFile.length() > 0L)

            val streamUri = localFile.toURI().toString()
            assertTrue("Stream URI should use file scheme", streamUri.startsWith("file:/"))
        } finally {
            tempAudio.delete()
        }
    }

    @Test
    fun testDownloadedSongModelProperties() {
        val downloadedSong = Song(
            id = "offline_master_001",
            title = "Hardware Groove",
            artist = "Synthesizer",
            album = "Deck A",
            durationSec = 300,
            artworkUrl = "https://example.com/art.jpg",
            stream160Url = "https://example.com/audio.mp3",
            stream320Url = "https://example.com/audio_hd.mp3",
            lyrics = "Analog beats running deep",
            isDownloaded = true,
            isFavorite = true,
            localFilePath = "/storage/emulated/0/Music/offline_master_001.mp3"
        )

        assertTrue("Song should be marked as downloaded", downloadedSong.isDownloaded)
        assertTrue("Song should be marked as favorite", downloadedSong.isFavorite)
        assertNotNull("Local file path should not be null", downloadedSong.localFilePath)
        assertEquals("Duration should be 300 seconds", 300, downloadedSong.durationSec)
        assertEquals("Title should match", "Hardware Groove", downloadedSong.title)
    }
}
