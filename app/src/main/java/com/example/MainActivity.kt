package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PulseMusicApp
import com.example.ui.theme.PulseMusicTheme
import com.example.ui.theme.SkeuoAmberGlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseMusicTheme(darkTheme = true) {
                KillSwitchWrapper {
                    PulseMusicApp()
                }
            }
        }
    }
}

@Composable
fun KillSwitchWrapper(content: @Composable () -> Unit) {
    var isKilled by remember { mutableStateOf(false) }
    var killMessage by remember { mutableStateOf("") }
    var hasChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://pulseapp-music.vercel.app/config.json")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val status = json.optString("status", "ACTIVE")
                    if (status == "KILLED") {
                        isKilled = true
                        killMessage = json.optString("message", "App discontinued.")
                    }
                }
            } catch (e: Exception) {
                // Ignore errors and fail open
            } finally {
                hasChecked = true
            }
        }
    }

    if (isKilled) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "APPLICATION TERMINATED",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    ),
                    color = SkeuoAmberGlow,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = killMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        content()
    }
}
