package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SpotifyAuthManager(private val context: Context) {

    companion object {
        val CLIENT_ID = com.example.BuildConfig.SPOTIFY_CLIENT_ID
        const val REDIRECT_URI = "pulsemusic://callback"
        const val AUTH_REQUEST_CODE = 1337
    }

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken

    fun authenticate(activity: Activity) {
        val builder = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            REDIRECT_URI
        )
        builder.setScopes(arrayOf("user-library-read", "playlist-read-private"))
        val request = builder.build()
        AuthorizationClient.openLoginActivity(activity, AUTH_REQUEST_CODE, request)
    }

    fun handleAuthResponse(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == AUTH_REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, intent)
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    // Successful response
                    _accessToken.value = response.accessToken
                    Log.d("SpotifyAuth", "Logged in successfully")
                }
                AuthorizationResponse.Type.ERROR -> {
                    // Handle error response
                    Log.e("SpotifyAuth", "Auth error: ${response.error}")
                }
                else -> {
                    // Most likely auth flow was cancelled
                    Log.d("SpotifyAuth", "Auth cancelled or unknown response")
                }
            }
        }
    }
    
    fun logout() {
        _accessToken.value = null
    }
}
