package com.example.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SongStreamUrlTest {

    @Test
    fun `http media urls are upgraded to https`() {
        val song = Song(stream320Url = "http://aac.saavncdn.com/320/track.mp4")

        assertEquals("https://aac.saavncdn.com/320/track.mp4", song.getStreamUrl(preferHighQuality = true))
    }

    @Test
    fun `https media urls are preserved`() {
        val song = Song(stream160Url = "https://example.com/track.mp3")

        assertEquals("https://example.com/track.mp3", song.getStreamUrl(preferHighQuality = true))
    }

    @Test
    fun `web pages are not treated as media`() {
        val song = Song(stream320Url = "https://www.jiosaavn.com/song/example")

        assertEquals("", song.getStreamUrl(preferHighQuality = true))
    }

    @Test
    fun `quality map is preferred over legacy fields`() {
        val song = Song(
            stream320Url = "https://example.com/legacy.mp3",
            downloadUrls = mapOf("320kbps" to "https://example.com/high.mp3")
        )

        assertEquals("https://example.com/high.mp3", song.getStreamUrl(preferHighQuality = true))
    }
}
