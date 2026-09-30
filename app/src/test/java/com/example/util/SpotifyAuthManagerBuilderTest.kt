package com.example.util
import org.junit.Test
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
class SpotifyAuthManagerBuilderTest {
    @Test fun testBuilder() {
        val b = AuthorizationRequest.Builder("id", AuthorizationResponse.Type.CODE, "uri")
        b.setCustomParam("test", "test")
    }
}
