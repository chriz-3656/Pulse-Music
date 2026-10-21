package com.example

import com.example.domain.model.Song
import com.example.domain.model.UserSettings
import com.example.ui.components.parseLrc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFeaturesTest {

    @Test
    fun testUserSettingsPureOfflineModeDefaultAndCopy() {
        val defaultSettings = UserSettings()
        assertFalse("Pure offline mode should be false by default", defaultSettings.isPureOfflineMode)

        val offlineEnabled = defaultSettings.copy(isPureOfflineMode = true)
        assertTrue("Pure offline mode should be true after update", offlineEnabled.isPureOfflineMode)
    }

    @Test
    fun testLyricParsingForStoryCardSelection() {
        val lrcContent = """
            [00:12.50]Here comes the sun
            [00:15.80]Little darlin'
            [00:18.20]It's been a long cold lonely winter
            [00:24.00]Here comes the sun
            [00:27.50]It's alright
        """.trimIndent()

        val parsed = parseLrc(lrcContent)
        assertEquals(5, parsed.size)
        assertEquals("Here comes the sun", parsed[0].text)
        assertEquals(12500L, parsed[0].timestampMs)
        assertEquals("It's alright", parsed[4].text)

        // Select a slice (max 5 lines for story card)
        val selected = parsed.take(3).map { it.text }
        assertEquals(3, selected.size)
        assertEquals("Here comes the sun", selected[0])
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
}
