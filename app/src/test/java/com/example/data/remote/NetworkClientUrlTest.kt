package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkClientUrlTest {

    @Test
    fun `custom base path is preserved and request path appended`() {
        val original = "https://music-api2.albatross0071.workers.dev/api/search?query=pulse".toHttpUrl()
        val result = NetworkClient.buildCustomApiUrl(original, "https://api.example.com/v1/")

        assertEquals("https://api.example.com/v1/api/search?query=pulse", result.toString())
    }

    @Test
    fun `duplicate boundary segment is not repeated`() {
        val original = "https://music-api2.albatross0071.workers.dev/api/search".toHttpUrl()
        val result = NetworkClient.buildCustomApiUrl(original, "https://api.example.com/api/")

        assertEquals("https://api.example.com/api/search", result.toString())
    }

    @Test
    fun `non https custom endpoint is rejected`() {
        val original = "https://music-api2.albatross0071.workers.dev/api/search".toHttpUrl()
        assertNull(NetworkClient.buildCustomApiUrl(original, "http://api.example.com/"))
    }

    @Test
    fun `invalid custom endpoint is rejected`() {
        val original = "https://music-api2.albatross0071.workers.dev/api/search".toHttpUrl()
        assertNull(NetworkClient.buildCustomApiUrl(original, "not a url"))
    }

    @Test
    fun `custom port is retained`() {
        val original = "https://music-api2.albatross0071.workers.dev/api/search".toHttpUrl()
        val result = NetworkClient.buildCustomApiUrl(original, "https://api.example.com:8443/")

        assertEquals("https://api.example.com:8443/api/search", result.toString())
    }
}
