package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class SpotifyLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Clear cookies before starting to ensure a fresh login if needed
        // CookieManager.getInstance().removeAllCookies(null)
        
        setContent {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        
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
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
