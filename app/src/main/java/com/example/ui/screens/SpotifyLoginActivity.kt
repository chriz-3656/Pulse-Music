package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.util.DebugLogger

class SpotifyLoginActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            com.example.ui.theme.PulseMusicTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Log in to Spotify") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                webChromeClient = WebChromeClient()
                                
                                var hasInjected = false
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView, url: String) {
                                        super.onPageFinished(view, url)
                                        DebugLogger.log("WebView onPageFinished: $url")
                                        if (hasInjected) return
                                        
                                        if (url.startsWith("https://open.spotify.com")) {
                                            val cookies = CookieManager.getInstance().getCookie("https://spotify.com") ?: CookieManager.getInstance().getCookie("https://open.spotify.com")
                                            if (cookies != null && cookies.contains("sp_dc=")) {
                                                val spDc = cookies.split(";").map { it.trim() }.firstOrNull { it.startsWith("sp_dc=") }?.substringAfter("=")
                                                if (!spDc.isNullOrBlank()) {
                                                    DebugLogger.log("On open.spotify.com, extracting session from DOM")
                                                    hasInjected = true
                                                    view.evaluateJavascript(
                                                        "try { " +
                                                        "  var b64 = document.querySelector('script[data-testid=\"session\"]').innerText;" +
                                                        "  var jsonStr = atob(b64);" +
                                                        "  window.SpotifyAuth.onToken(jsonStr, '$spDc');" +
                                                        "} catch(e) { window.SpotifyAuth.onToken('error: ' + e.message, '$spDc'); }"
                                                    , null)
                                                }
                                            }
                                        }
                                    }
                                }
                                addJavascriptInterface(object : Any() {
                                    @android.webkit.JavascriptInterface
                                    fun onToken(jsonStr: String, spDc: String) {
                                        DebugLogger.log("JS onToken received, length: ${jsonStr.length}")
                                        val result = Intent().apply { 
                                            putExtra("sp_dc", spDc)
                                            putExtra("token_json", jsonStr)
                                        }
                                        setResult(Activity.RESULT_OK, result)
                                        finish()
                                    }
                                }, "SpotifyAuth")
                                loadUrl("https://accounts.spotify.com/en/login?continue=https%3A%2F%2Fopen.spotify.com%2F")
                            }
                        },
                        modifier = Modifier.fillMaxSize().padding(paddingValues)
                    )
                }
            }
        }
    }
}
