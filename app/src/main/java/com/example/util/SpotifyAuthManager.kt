package com.example.util

import kotlinx.coroutines.launch
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
        val CLIENT_SECRET = com.example.BuildConfig.SPOTIFY_CLIENT_SECRET
        const val REDIRECT_URI = "pulsemusic://callback"
        const val AUTH_REQUEST_CODE = 1337
    }

    private val prefs = context.getSharedPreferences("spotify_auth", Context.MODE_PRIVATE)
    private val _accessToken = MutableStateFlow<String?>(prefs.getString("access_token", null))
    val accessToken: StateFlow<String?> = _accessToken

    private fun saveToken(token: String?) {
        saveToken(token)
        prefs.edit().putString("access_token", token).apply()
    }

    fun authenticate(activity: Activity) {
        val builder = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.CODE,
            REDIRECT_URI
        )
        builder.setScopes(arrayOf("user-library-read", "playlist-read-private", "user-read-private", "user-read-email"))
        val request = builder.build()
        AuthorizationClient.openLoginActivity(activity, AUTH_REQUEST_CODE, request)
    }

    fun handleAuthResponse(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == AUTH_REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, intent)
            when (response.type) {
                AuthorizationResponse.Type.CODE -> {
                    // Exchange code for token
                    exchangeCodeForToken(response.code)
                }
                AuthorizationResponse.Type.TOKEN -> {
                    saveToken(response.accessToken)
                    Log.d("SpotifyAuth", "Logged in successfully with implicit token")
                }
                AuthorizationResponse.Type.ERROR -> {
                    Log.e("SpotifyAuth", "Auth error: ${response.error}")
                }
                else -> {
                    Log.d("SpotifyAuth", "Auth cancelled or unknown response")
                }
            }
        }
    }
    
    private fun exchangeCodeForToken(code: String) {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                val authString = android.util.Base64.encodeToString(
                    "$CLIENT_ID:$CLIENT_SECRET".toByteArray(),
                    android.util.Base64.NO_WRAP
                )
                val requestBody = okhttp3.FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", code)
                    .add("redirect_uri", REDIRECT_URI)
                    .build()
                    
                val request = okhttp3.Request.Builder()
                    .url("https://accounts.spotify.com/api/token")
                    .post(requestBody)
                    .addHeader("Authorization", "Basic $authString")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                    
                val response = client.newCall(request).execute()
                val bodyStr = response.body?.string()
                
                if (response.isSuccessful && bodyStr != null) {
                    val json = org.json.JSONObject(bodyStr)
                    val token = json.getString("access_token")
                    saveToken(token)
                    Log.d("SpotifyAuth", "Successfully exchanged code for token")
                } else {
                    Log.e("SpotifyAuth", "Failed to exchange token: $bodyStr")
                }
            } catch (e: Exception) {
                Log.e("SpotifyAuth", "Error exchanging token", e)
            }
        }
    }
    
    fun logout() {
        saveToken(null)
    }
}
