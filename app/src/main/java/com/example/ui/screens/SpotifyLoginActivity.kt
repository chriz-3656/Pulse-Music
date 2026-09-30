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
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                webChromeClient = WebChromeClient()
                                
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        val cookies = CookieManager.getInstance().getCookie("https://spotify.com")
                                        if (cookies != null && cookies.contains("sp_dc=")) {
                                            val spDc = cookies.split(";").map { it.trim() }.firstOrNull { it.startsWith("sp_dc=") }?.substringAfter("=")
                                            if (!spDc.isNullOrBlank()) {
                                                val result = Intent().apply { putExtra("sp_dc", spDc) }
                                                setResult(Activity.RESULT_OK, result)
                                                finish()
                                            }
                                        }
                                    }
                                }
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
