package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class SpotifyAuthManagerTest {

    private lateinit var context: Context
    private lateinit var authManager: SpotifyAuthManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        authManager = SpotifyAuthManager(context)
    }

    @Test
    fun testInitialState() = runBlocking {
        assertNull("Access token should be null initially", authManager.accessToken.first())
    }

    @Test
    fun testLogoutClearsToken() = runBlocking {
        authManager.logout()
        assertNull("Access token should be null after logout", authManager.accessToken.first())
    }
}
