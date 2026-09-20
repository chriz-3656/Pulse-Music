package com.example.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppUpdater {
    private const val GITHUB_API_URL = "https://api.github.com/repos/chriz-3656/Pulse-Music/releases/latest"

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val tagName = json.getString("tag_name")
                val cleanTagName = tagName.removePrefix("v")
                val currentVersion = BuildConfig.VERSION_NAME
                
                val isUpdateAvailable = isVersionGreater(cleanTagName, currentVersion)
                
                var downloadUrl = ""
                val assets = json.getJSONArray("assets")
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.endsWith(".apk") && !name.contains("-debug")) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }
                
                return@withContext UpdateInfo(
                    isUpdateAvailable = isUpdateAvailable && downloadUrl.isNotBlank(),
                    latestVersion = tagName,
                    downloadUrl = downloadUrl,
                    releaseNotes = json.optString("body", "Bug fixes and improvements.")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    private fun isVersionGreater(v1: String, v2: String): Boolean {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 > p2) return true
            if (p1 < p2) return false
        }
        return false
    }

    fun downloadAndInstall(context: Context, downloadUrl: String, fileName: String): Long {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(downloadUrl)
            
            val request = DownloadManager.Request(uri)
                .setTitle("Pulse Music Update")
                .setDescription("Downloading latest update...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        installApk(context, fileName)
                        context.unregisterReceiver(this)
                    }
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }
            return downloadId
        } catch (e: Exception) {
            e.printStackTrace()
            return -1L
        }
    }

    private fun installApk(context: Context, fileName: String) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (!file.exists()) return

        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(intent)
    }

    fun getObsoleteApks(): List<File> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val files = downloadsDir.listFiles { _, name ->
            name.startsWith("PulseMusic-") && name.endsWith(".apk")
        }
        return files?.toList() ?: emptyList()
    }

    fun deleteApk(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }
}
