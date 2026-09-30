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
            val tokenJson = intent?.getStringExtra("token_json")
            if (!spDc.isNullOrBlank() && !tokenJson.isNullOrBlank()) {
                try {
                    val json = org.json.JSONObject(tokenJson)
                    val token = json.optString("accessToken")
                    if (token.isNotBlank()) {
                        saveToken(token)
                        prefs.edit().putString("sp_dc", spDc).apply()
                        Log.d("SpotifyAuth", "Successfully fetched Web Player token via WebView")
                    } else {
                        fetchWebPlayerToken(spDc)
                    }
                } catch (e: Exception) {
                    fetchWebPlayerToken(spDc)
                }
            } else {
                Log.e("SpotifyAuth", "sp_dc or token_json was null or blank")
            }
        }
    }
    
    private fun fetchWebPlayerToken(spDc: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                val webView = android.webkit.WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: android.webkit.WebView, url: String) {
                            if (url.contains("get_access_token")) {
                                view.evaluateJavascript(
                                    "document.documentElement.innerText;"
                                ) { jsonStr ->
                                    try {
                                        // jsonStr comes surrounded by escaped quotes if it's a string, so we clean it up safely
                                        val cleanJson = if (jsonStr != null && jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                                            jsonStr.substring(1, jsonStr.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
                                        } else {
                                            jsonStr ?: ""
                                        }
                                        val json = org.json.JSONObject(cleanJson)
                                        val token = json.optString("accessToken")
                                        if (token.isNotBlank()) {
                                            saveToken(token)
                                            Log.d("SpotifyAuth", "Successfully refreshed Web Player token in background")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SpotifyAuth", "Failed to parse refresh token", e)
                                    }
                                }
                            }
                        }
                    }
                }
                android.webkit.CookieManager.getInstance().setCookie("https://spotify.com", "sp_dc=$spDc")
                android.webkit.CookieManager.getInstance().flush()
                webView.loadUrl("https://open.spotify.com/get_access_token?reason=transport&productType=web_player")
            } catch (e: Exception) {
                Log.e("SpotifyAuth", "Error starting background WebView", e)
            }
        }
    }
    
    fun logout() {
        saveToken(null)
        prefs.edit().remove("sp_dc").apply()
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
    }
}
