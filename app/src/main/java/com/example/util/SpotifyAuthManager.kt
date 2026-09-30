package com.example.util

import kotlinx.coroutines.launch
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SpotifyAuthManager(private val context: Context) {

    companion object {
        const val AUTH_REQUEST_CODE = 1337
    }

    private val prefs = context.getSharedPreferences("spotify_auth", Context.MODE_PRIVATE)
    private val _accessToken = MutableStateFlow<String?>(prefs.getString("access_token", null))
    val accessToken: StateFlow<String?> = _accessToken

    init {
        // Automatically refresh token on startup if sp_dc is available
        val spDc = prefs.getString("sp_dc", null)
        if (!spDc.isNullOrBlank()) {
            fetchWebPlayerToken(spDc)
        }
    }

    fun saveToken(token: String?) {
        _accessToken.value = token
        prefs.edit().putString("access_token", token).apply()
    }

    fun authenticate(activity: Activity) {
        val intent = Intent(activity, com.example.ui.screens.SpotifyLoginActivity::class.java)
        activity.startActivityForResult(intent, AUTH_REQUEST_CODE)
    }

    fun handleAuthResponse(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == AUTH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val spDc = intent?.getStringExtra("sp_dc")
            if (!spDc.isNullOrBlank()) {
                fetchWebPlayerToken(spDc)
            } else {
                Log.e("SpotifyAuth", "sp_dc was null or blank")
            }
        }
    }
    
    private fun fetchWebPlayerToken(spDc: String) {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://open.spotify.com/get_access_token?reason=transport&productType=web_player")
                    .addHeader("Cookie", "sp_dc=$spDc")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Language", "en-US,en;q=0.9")
                    .addHeader("App-Platform", "WebPlayer")
                    .build()
                    
                val response = client.newCall(request).execute()
                val bodyStr = response.body?.string()
                
                if (response.isSuccessful && bodyStr != null) {
                    val json = org.json.JSONObject(bodyStr)
                    val token = json.optString("accessToken")
                    if (token.isNotBlank()) {
                        saveToken(token)
                        prefs.edit().putString("sp_dc", spDc).apply()
                        Log.d("SpotifyAuth", "Successfully fetched Web Player token")
                    } else {
                        Log.e("SpotifyAuth", "No accessToken in response: $bodyStr")
                    }
                } else {
                    Log.e("SpotifyAuth", "Failed to fetch Web token: $bodyStr")
                }
            } catch (e: Exception) {
                Log.e("SpotifyAuth", "Error fetching Web token", e)
            }
        }
    }
    
    fun logout() {
        saveToken(null)
        prefs.edit().remove("sp_dc").apply()
    }
}
