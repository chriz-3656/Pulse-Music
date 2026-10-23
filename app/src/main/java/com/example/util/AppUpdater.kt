package com.example.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object AppUpdater {
    private const val GITHUB_API_URL =
        "https://api.github.com/repos/chriz-3656/Pulse-Music/releases/latest"
    private const val UPDATE_DIR = "updates"

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "PulseMusic/${BuildConfig.VERSION_NAME}")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val tagName = json.optString("tag_name").trim()
                if (tagName.isBlank()) return@withContext null

                val cleanTagName = tagName.removePrefix("v")
                val isUpdateAvailable = isVersionGreater(cleanTagName, BuildConfig.VERSION_NAME)

                var downloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name")
                        val candidate = asset.optString("browser_download_url")
                        if (
                            name.endsWith(".apk", ignoreCase = true) &&
                            !name.contains("-debug", ignoreCase = true) &&
                            isHttpsUrl(candidate)
                        ) {
                            downloadUrl = candidate
                            break
                        }
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
        } finally {
            connection?.disconnect()
        }
        null
    }

    internal fun isVersionGreater(candidate: String, current: String): Boolean {
        val parts1 = candidate.removePrefix("v").split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val parts2 = current.removePrefix("v").split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
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
        if (!isHttpsUrl(downloadUrl)) {
            showToast(context, "Update failed: download URL is not secure.")
            return -1L
        }

        val safeFileName = sanitizeApkFileName(fileName)
        return try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("Pulse Music Update")
                .setDescription("Downloading latest update...")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    safeFileName
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id != downloadId) return

                    try {
                        receiverContext.unregisterReceiver(this)
                    } catch (_: Exception) {
                        // Already unregistered.
                    }

                    val apk = copyDownloadedApkToCache(receiverContext, downloadId, safeFileName)
                    if (apk == null) {
                        showToast(receiverContext, "Update failed: the download could not be verified.")
                        return
                    }
                    installApk(receiverContext, apk)
                }
            }

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(onComplete, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(onComplete, filter)
            }
            downloadId
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(context, "Update failed: unable to start the download.")
            -1L
        }
    }

    private fun copyDownloadedApkToCache(
        context: Context,
        downloadId: Long,
        expectedFileName: String
    ): File? {
        return try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)
            var localUri: String? = null
            var status = DownloadManager.STATUS_FAILED

            downloadManager.query(query).use { cursor ->
                if (!cursor.moveToFirst()) return null
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                if (statusIndex >= 0) status = cursor.getInt(statusIndex)
                if (uriIndex >= 0) localUri = cursor.getString(uriIndex)
            }

            if (status != DownloadManager.STATUS_SUCCESSFUL) return null

            val downloadedFile = localUri
                ?.takeIf { it.startsWith("file:", ignoreCase = true) }
                ?.let { uri -> Uri.parse(uri).path?.let(::File) }
                ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?.let { File(it, expectedFileName) }
                ?: return null

            if (!downloadedFile.isFile || downloadedFile.length() <= 0L) return null

            val updateDir = File(context.cacheDir, UPDATE_DIR).apply { mkdirs() }
            val safeFile = File(updateDir, sanitizeApkFileName(expectedFileName))
            downloadedFile.inputStream().use { input ->
                safeFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (safeFile.length() <= 0L) return null
            safeFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        if (!isValidUpdate(context, apkFile)) {
            showToast(context, "Update failed: signature or version verification failed.")
            return
        }

        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast(context, "Update failed: unable to open the installer.")
        }
    }

    /**
     * Verifies that the downloaded APK:
     *  - belongs to this package,
     *  - is newer than the installed build,
     *  - is signed by the same certificate as the installed build.
     *
     * This method fails closed: any exception or missing signature is a rejection.
     */
    private fun isValidUpdate(context: Context, apkFile: File): Boolean {
        return try {
            val pm = context.packageManager
            val currentInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNATURES)
            } ?: return false

            if (archiveInfo.packageName != context.packageName) return false
            if (PackageInfoCompat.getLongVersionCode(archiveInfo) <=
                PackageInfoCompat.getLongVersionCode(currentInfo)
            ) {
                return false
            }

            val currentSigners = signerDigests(currentInfo)
            val archiveSigners = signerDigests(archiveInfo)
            currentSigners.isNotEmpty() &&
                archiveSigners.isNotEmpty() &&
                currentSigners.any(archiveSigners::contains)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun signerDigests(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures ?: return emptySet()
        }

        return signatures
            .mapNotNull { signature -> signature.toByteArray().sha256Hex() }
            .toSet()
    }

    private fun ByteArray.sha256Hex(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this)
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun isHttpsUrl(value: String): Boolean {
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }

    private fun sanitizeApkFileName(fileName: String): String {
        val base = File(fileName).name
        val cleaned = base.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return if (cleaned.endsWith(".apk", ignoreCase = true)) cleaned else "$cleaned.apk"
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun getObsoleteApks(context: Context): List<File> {
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: return emptyList()
        val files = downloadsDir.listFiles { _, name ->
            name.startsWith("PulseMusic-") && name.endsWith(".apk", ignoreCase = true)
        }
        return files?.toList() ?: emptyList()
    }

    fun deleteApk(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }
}
