package com.example

import com.example.domain.model.Song
import com.example.domain.model.ThemeColorway
import com.example.domain.model.UserSettings
import com.example.ui.components.parseLrc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFeaturesTest {

    @Test
    fun testThemeColorwayResolutionAndDefaults() {
        // Test default UserSettings theme colorway
        val defaultSettings = UserSettings()
        assertEquals(ThemeColorway.AMBER, defaultSettings.themeColorway)

        // Test theme copy
        val cyanSettings = defaultSettings.copy(themeColorway = ThemeColorway.CYAN)
        assertEquals(ThemeColorway.CYAN, cyanSettings.themeColorway)

        val greenSettings = defaultSettings.copy(themeColorway = ThemeColorway.GREEN)
        assertEquals(ThemeColorway.GREEN, greenSettings.themeColorway)

        // Test fromId resolution
        assertEquals(ThemeColorway.AMBER, ThemeColorway.fromId("amber"))
        assertEquals(ThemeColorway.CYAN, ThemeColorway.fromId("cyan"))
        assertEquals(ThemeColorway.GREEN, ThemeColorway.fromId("green"))
        assertEquals(ThemeColorway.CYAN, ThemeColorway.fromId("CYAN"))

        // Test fallback for unknown / empty ID
        assertEquals(ThemeColorway.AMBER, ThemeColorway.fromId("unknown"))
        assertEquals(ThemeColorway.AMBER, ThemeColorway.fromId(""))

        // Verify hex accents
        assertEquals(0xFFFFC107, ThemeColorway.AMBER.hexAccent)
        assertEquals(0xFF00D9FF, ThemeColorway.CYAN.hexAccent)
        assertEquals(0xFF7CFF5B, ThemeColorway.GREEN.hexAccent)
    }

    @Test
    fun testUserSettingsPureOfflineModeDefaultAndCopy() {
        val defaultSettings = UserSettings()
        assertFalse("Pure offline mode should be false by default", defaultSettings.isPureOfflineMode)

        val offlineEnabled = defaultSettings.copy(isPureOfflineMode = true)
        assertTrue("Pure offline mode should be true after update", offlineEnabled.isPureOfflineMode)
    }

    @Test
    fun testPlainTextLyricsParsingFallback() {
        val plainLyrics = "Line 1 of song\nLine 2 of song\nLine 3 of song"
        val parsed = parseLrc(plainLyrics)
        assertEquals(3, parsed.size)
        assertEquals("Line 1 of song", parsed[0].text)
        assertEquals("Line 2 of song", parsed[1].text)
    }

    @Test
    fun testBatchDownloadStateVerification() {
        val songs = listOf(
            Song(id = "s1", title = "Track 1", isDownloaded = true),
            Song(id = "s2", title = "Track 2", isDownloaded = true),
            Song(id = "s3", title = "Track 3", isDownloaded = false)
        )

        val downloadedIds = setOf("s1", "s2")
        val isAllDownloadedBefore = songs.all { it.id in downloadedIds || it.isDownloaded }
        assertFalse("Not all songs should be downloaded", isAllDownloadedBefore)

        val fullDownloadedIds = setOf("s1", "s2", "s3")
        val isAllDownloadedAfter = songs.all { it.id in fullDownloadedIds || it.isDownloaded }
        assertTrue("All songs should be recognized as downloaded", isAllDownloadedAfter)
    }

    @Test
    fun testDownloadTaskQueueAndLiveProgress() {
        val song1 = Song(id = "track_01", title = "Midnight Ride", artist = "Synth Wave")
        val song2 = Song(id = "track_02", title = "Retro Sunset", artist = "Neon Pulse")

        val task1 = com.example.domain.model.DownloadTask(
            song = song1,
            progress = 0.45f,
            bytesDownloaded = 4500L,
            totalBytes = 10000L,
            status = com.example.domain.model.DownloadStatus.DOWNLOADING,
            sourceCollection = "Playlist: Synthwave Classics"
        )

        val task2 = com.example.domain.model.DownloadTask(
            song = song2,
            progress = 0f,
            status = com.example.domain.model.DownloadStatus.QUEUED,
            sourceCollection = "Playlist: Synthwave Classics"
        )

        val tasks = listOf(task1, task2)
        assertEquals(2, tasks.size)
        assertEquals(com.example.domain.model.DownloadStatus.DOWNLOADING, tasks[0].status)
        assertEquals(0.45f, tasks[0].progress, 0.001f)
        assertEquals(com.example.domain.model.DownloadStatus.QUEUED, tasks[1].status)
        assertEquals("Playlist: Synthwave Classics", tasks[0].sourceCollection)
    }
}
