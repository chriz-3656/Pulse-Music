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
                DebugLogger.log("SpotifyAuth: handling JS token response")
                try {
                    val json = org.json.JSONObject(tokenJson)
                    val token = json.optString("accessToken")
                    if (token.isNotBlank()) {
                        saveToken(token)
                        prefs.edit().putString("sp_dc", spDc).apply()
                        DebugLogger.log("SpotifyAuth: Successfully fetched Web Player token via WebView")
                    } else {
                        DebugLogger.log("SpotifyAuth ERROR: token JSON missing accessToken")
                        fetchWebPlayerToken(spDc)
                    }
                } catch (e: Exception) {
                    DebugLogger.log("SpotifyAuth ERROR: exception parsing token JSON: ${e.message}")
                    fetchWebPlayerToken(spDc)
                }
            } else {
                DebugLogger.log("SpotifyAuth ERROR: sp_dc or token_json was null or blank")
            }
        }
    }
    
    private fun fetchWebPlayerToken(spDc: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                DebugLogger.log("SpotifyAuth: starting background WebView for refresh")
                val webView = android.webkit.WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    
                    addJavascriptInterface(object : Any() {
                        @android.webkit.JavascriptInterface
                        fun onToken(jsonStr: String) {
                            DebugLogger.log("SpotifyAuth: background JS onToken received")
                            try {
                                val json = org.json.JSONObject(jsonStr)
                                val token = json.optString("accessToken")
                                if (token.isNotBlank()) {
                                    saveToken(token)
                                    DebugLogger.log("SpotifyAuth: Successfully refreshed Web Player token in background")
                                } else {
                                    DebugLogger.log("SpotifyAuth ERROR: refreshed JSON missing accessToken")
                                }
                            } catch (e: Exception) {
                                DebugLogger.log("SpotifyAuth ERROR: Failed to parse refresh token: ${e.message}")
                            }
                        }
                    }, "SpotifyAuth")
                }
                android.webkit.CookieManager.getInstance().setCookie("https://spotify.com", "sp_dc=$spDc")
                android.webkit.CookieManager.getInstance().setCookie("https://open.spotify.com", "sp_dc=$spDc")
                android.webkit.CookieManager.getInstance().flush()
                
                val html = "<html><body><script>fetch('https://open.spotify.com/get_access_token?reason=transport&productType=web_player', {headers: {'Accept': 'application/json'}}).then(r=>r.text()).then(t=>window.SpotifyAuth.onToken(t)).catch(e=>window.SpotifyAuth.onToken('error: ' + e));</script></body></html>"
                webView.loadDataWithBaseURL("https://open.spotify.com/", html, "text/html", "UTF-8", "https://open.spotify.com/auth_hack")
            } catch (e: Exception) {
                DebugLogger.log("SpotifyAuth ERROR: Error starting background WebView: ${e.message}")
            }
        }
    }
    
    fun logout() {
        saveToken(null)
        prefs.edit().remove("sp_dc").apply()
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        DebugLogger.log("SpotifyAuth: Logged out")
    }
}
