package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.Song
import com.example.player.MusicPlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AutoplayAndRadioTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context

    private val seedSong = Song(
        id = "seed_track_01",
        title = "Midnight Horizon",
        artist = "Synth Wave Ensemble",
        album = "Analog Dreams",
        durationSec = 210
    )

    private val recommendedSong1 = Song(
        id = "reco_track_02",
        title = "Neon Highway",
        artist = "Retrowave Unit",
        album = "Outrun 84",
        durationSec = 195
    )

    private val recommendedSong2 = Song(
        id = "reco_track_03",
        title = "Cyber Sunrise",
        artist = "Laser Matrix",
        album = "Future Past",
        durationSec = 240
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSingleTrackSeedsAutoplayQueue() = runTest(testDispatcher) {
        val controller = MusicPlayerController(context)
        controller.isAutoplayEnabled = true

        var providerCalled = false
        var requestedSongId: String? = null

        controller.autoplayProvider = { songId ->
            providerCalled = true
            requestedSongId = songId
            listOf(recommendedSong1, recommendedSong2)
        }

        // Play single song as radio seed
        controller.playSong(seedSong, listOf(seedSong))

        // Advance coroutines in controller's scope
        advanceUntilIdle()

        assertTrue("Autoplay provider should have been invoked", providerCalled)
        assertEquals("Seed song ID should match requested ID", seedSong.id, requestedSongId)

        val queue = controller.playerState.value.queue
        assertEquals("Queue should have seed + 2 recommendations", 3, queue.size)
        assertEquals("First track in queue must be the seed song", seedSong.id, queue[0].id)
        assertEquals("Second track in queue must be recommended track 1", recommendedSong1.id, queue[1].id)
        assertEquals("Third track in queue must be recommended track 2", recommendedSong2.id, queue[2].id)
    }

    @Test
    fun testAutoplayFiltersOutDuplicateSeedSong() = runTest(testDispatcher) {
        val controller = MusicPlayerController(context)
        controller.isAutoplayEnabled = true

        controller.autoplayProvider = {
            // Recommendation API returns seed song along with other tracks
            listOf(seedSong, recommendedSong1, recommendedSong2)
        }

        controller.playSong(seedSong, listOf(seedSong))
        advanceUntilIdle()

        val queue = controller.playerState.value.queue
        // Duplicate seed track must be filtered out
        val seedOccurrences = queue.count { it.id == seedSong.id }
        assertEquals("Seed song should appear exactly once in queue", 1, seedOccurrences)
        assertEquals(3, queue.size)
    }

    @Test
    fun testMultiTrackExplicitQueueDoesNotTriggerAutoplayInitially() = runTest(testDispatcher) {
        val controller = MusicPlayerController(context)
        controller.isAutoplayEnabled = true

        var providerCalled = false
        controller.autoplayProvider = {
            providerCalled = true
            listOf(recommendedSong1)
        }

        val albumTracks = listOf(seedSong, recommendedSong1, recommendedSong2)
        // User plays an album/playlist with multiple tracks
        controller.playSong(seedSong, albumTracks)
        advanceUntilIdle()

        assertFalse("Autoplay provider should NOT trigger when queue already has multiple tracks", providerCalled)
        assertEquals("Original queue size must remain unchanged", 3, controller.playerState.value.queue.size)
    }

    @Test
    fun testAutoplayDisabledDoesNotFetchRecommendations() = runTest(testDispatcher) {
        val controller = MusicPlayerController(context)
        controller.isAutoplayEnabled = false

        var providerCalled = false
        controller.autoplayProvider = {
            providerCalled = true
            listOf(recommendedSong1)
        }

        controller.playSong(seedSong, listOf(seedSong))
        advanceUntilIdle()

        assertFalse("Autoplay should not fetch when disabled", providerCalled)
        assertEquals(1, controller.playerState.value.queue.size)
    }

    @Test
    fun testSkipToNextAtQueueEndFetchesAutoplayAndContinues() = runTest(testDispatcher) {
        val controller = MusicPlayerController(context)
        controller.isAutoplayEnabled = true

        controller.autoplayProvider = { seed ->
            listOf(recommendedSong1, recommendedSong2)
        }

        // Initialize with a 2-track queue
        controller.playSong(seedSong, listOf(seedSong, recommendedSong1))
        advanceUntilIdle()

        // Skip to track 2
        controller.skipToNext()
        assertEquals(recommendedSong1.id, controller.playerState.value.currentSong?.id)

        // Queue now triggers continuous playback or pre-fetch when advancing
        advanceUntilIdle()
        assertTrue("Queue should have expanded with recommendations", controller.playerState.value.queue.size >= 3)
    }

    @Test
    fun testAddToQueueAndPlayNext() = runTest(testDispatcher) {
        val controller = MusicPlayerController(context)
        controller.isAutoplayEnabled = false

        controller.playSong(seedSong, listOf(seedSong))
        assertEquals(1, controller.playerState.value.queue.size)

        // Add to queue
        controller.addToQueue(recommendedSong2)
        assertEquals(2, controller.playerState.value.queue.size)
        assertEquals(recommendedSong2.id, controller.playerState.value.queue[1].id)

        // Play next (inserts right after current song)
        controller.playNext(recommendedSong1)
        assertEquals(3, controller.playerState.value.queue.size)
        assertEquals(seedSong.id, controller.playerState.value.queue[0].id)
        assertEquals(recommendedSong1.id, controller.playerState.value.queue[1].id)
        assertEquals(recommendedSong2.id, controller.playerState.value.queue[2].id)
    }
}
