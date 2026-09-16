package com.example.data.remote

import android.util.Log
import com.example.domain.model.Song
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * SoundCloud API-v2 client with dynamic client_id extraction and automatic 401 recovery.
 * Mirrors the stream extraction flow from the user's SoundCloud Radio script:
 * - Dynamically extracts a fresh client_id from soundcloud.com asset JS bundles
 * - Attaches browser headers (User-Agent, Referer, Origin, Accept) to bypass 401/WAF blocks
 * - Extracts direct progressive MP3 / AAC and HLS stream URLs via media transcodings and track_authorization
 */
object SoundCloudClient {
    private const val TAG = "SoundCloudClient"
    private const val API_BASE = "https://api-v2.soundcloud.com"
    private const val CLIENT_ID_CACHE_TTL_MS = 3 * 3600 * 1000L // 3 hours

    // Fallback client IDs known to be active
    private val FALLBACK_CLIENT_IDS = listOf(
        "Pb72ranhoyt6gw7hM7TkzUItXlMWSNSo",
        "iZIs9mchVcX5lhVR1OiZAkGCUgu2K642",
        "a3e059563d7fd3372b49b37f00a00bcf"
    )

    private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    @Volatile
    private var cachedClientId: String? = null
    @Volatile
    private var lastClientIdFetchTime = 0L

    // In-memory cache for track transcodings and authorization tokens to make playback instant
    private data class CachedTrackInfo(
        val rawId: String,
        val title: String,
        val artist: String,
        val trackAuth: String,
        val transcodings: List<TranscodingInfo>
    )

    data class TranscodingInfo(
        val url: String,
        val protocol: String,
        val mimeType: String,
        val isPreview: Boolean
    )

    private val trackCache = ConcurrentHashMap<String, CachedTrackInfo>()
    private val resolvedStreamCache = ConcurrentHashMap<String, Pair<String, Long>>() // url -> (streamUrl, timestamp)

    /**
     * Get an active, verified client_id.
     * If cached client_id is missing or expired, fetches fresh client_id from soundcloud.com JS bundles.
     */
    @Synchronized
    fun getClientId(forceRefresh: Boolean = false): String {
        val now = System.currentTimeMillis()
        val current = cachedClientId
        if (!forceRefresh && current != null && (now - lastClientIdFetchTime < CLIENT_ID_CACHE_TTL_MS)) {
            return current
        }

        Log.d(TAG, "Fetching fresh SoundCloud client_id from web assets...")
        val freshId = extractClientIdFromWeb()
        if (!freshId.isNullOrBlank()) {
            cachedClientId = freshId
            lastClientIdFetchTime = now
            Log.d(TAG, "Successfully extracted fresh SoundCloud client_id: $freshId")
            return freshId
        }

        // Return first fallback if dynamic extraction fails
        val fallback = FALLBACK_CLIENT_IDS.first()
        cachedClientId = fallback
        lastClientIdFetchTime = now
        Log.w(TAG, "Using fallback SoundCloud client_id: $fallback")
        return fallback
    }

    /**
     * Scrapes soundcloud.com homepage to find asset bundle JS files,
     * then scans bundles for the active client_id, exactly like the radio script.
     */
    private fun extractClientIdFromWeb(): String? {
        try {
            val homeConn = URL("https://soundcloud.com").openConnection() as HttpURLConnection
            homeConn.setRequestProperty("User-Agent", USER_AGENT)
            homeConn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            homeConn.connectTimeout = 5000
            homeConn.readTimeout = 5000
            
            if (homeConn.responseCode != 200) {
                Log.w(TAG, "Failed to fetch soundcloud.com homepage: HTTP ${homeConn.responseCode}")
                return null
            }

            val html = homeConn.inputStream.bufferedReader().readText()
            
            // Regex to find bundle URLs: https://a-v2.sndcdn.com/assets/[^"]+\.js
            val bundlePattern = Pattern.compile("https://a-v2\\.sndcdn\\.com/assets/[^\"']+\\.js")
            val matcher = bundlePattern.matcher(html)
            val bundles = mutableListOf<String>()
            while (matcher.find()) {
                val b = matcher.group()
                if (!bundles.contains(b)) {
                    bundles.add(b)
                }
            }

            if (bundles.isEmpty()) {
                Log.w(TAG, "No JS bundle URLs found on SoundCloud homepage")
                return null
            }

            // Patterns to extract client_id inside bundles
            val cidPatterns = listOf(
                Pattern.compile("\"client_id\":\"([a-zA-Z0-9]{32})\""),
                Pattern.compile("client_id:\"([a-zA-Z0-9]{32})\""),
                Pattern.compile("client_id=([a-zA-Z0-9]{32})"),
                Pattern.compile("client_id:\\s*\"([a-zA-Z0-9]{32})\"")
            )

            // Reverse iterate as main application bundles are usually near the bottom
            for (bundleUrl in bundles.reversed()) {
                try {
                    val bConn = URL(bundleUrl).openConnection() as HttpURLConnection
                    bConn.setRequestProperty("User-Agent", USER_AGENT)
                    bConn.connectTimeout = 4000
                    bConn.readTimeout = 4000
                    if (bConn.responseCode == 200) {
                        val js = bConn.inputStream.bufferedReader().readText()
                        for (pat in cidPatterns) {
                            val cidMatcher = pat.matcher(js)
                            if (cidMatcher.find()) {
                                val cid = cidMatcher.group(1)
                                if (!cid.isNullOrBlank() && cid.length == 32) {
                                    return cid
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Try next bundle
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting client_id from SoundCloud web", e)
        }
        return null
    }

    /**
     * Executes HTTP GET with required SoundCloud headers (Referer, Origin, User-Agent).
     * If HTTP 401 Unauthorized occurs, automatically forces client_id refresh and retries once.
     */
    private fun executeGet(urlStr: String, retryOn401: Boolean = true): Pair<Int, String?> {
        try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json, text/plain, */*")
            conn.setRequestProperty("Referer", "https://soundcloud.com/")
            conn.setRequestProperty("Origin", "https://soundcloud.com")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val code = conn.responseCode
            if (code in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val body = reader.readText()
                reader.close()
                return Pair(code, body)
            } else if (code == 401 && retryOn401) {
                Log.w(TAG, "Received 401 from SoundCloud for $urlStr. Refreshing client_id and retrying...")
                val currentCid = cachedClientId ?: ""
                val freshCid = getClientId(forceRefresh = true)
                val newUrlStr = if (currentCid.isNotBlank() && urlStr.contains(currentCid)) {
                    urlStr.replace(currentCid, freshCid)
                } else if (urlStr.contains("client_id=")) {
                    urlStr.replace(Regex("client_id=[a-zA-Z0-9]+"), "client_id=$freshCid")
                } else {
                    if (urlStr.contains("?")) "$urlStr&client_id=$freshCid" else "$urlStr?client_id=$freshCid"
                }
                return executeGet(newUrlStr, retryOn401 = false)
            } else {
                Log.w(TAG, "SoundCloud HTTP request failed with code $code for $urlStr")
                return Pair(code, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception in executeGet for $urlStr", e)
            return Pair(-1, null)
        }
    }

    /**
     * Search SoundCloud tracks by query.
     */
    fun searchTracks(query: String, limit: Int = 20): List<Song> {
        val songs = mutableListOf<Song>()
        if (query.isBlank()) return songs

        try {
            val clientId = getClientId()
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "$API_BASE/search/tracks?q=$encoded&client_id=$clientId&limit=$limit"
            
            val (code, body) = executeGet(url)
            if (code == 200 && !body.isNullOrBlank()) {
                val json = JSONObject(body)
                val collection = json.optJSONArray("collection") ?: return songs
                
                for (i in 0 until collection.length()) {
                    val track = collection.getJSONObject(i)
                    val rawId = track.optString("id")
                    val title = track.optString("title")
                    if (rawId.isBlank() || title.isBlank()) continue

                    val userObj = track.optJSONObject("user")
                    val artist = userObj?.optString("username") ?: "SoundCloud Artist"
                    val durationMs = track.optInt("duration", 0)
                    var artworkUrl = track.optString("artwork_url", "")
                    if (artworkUrl.isNotBlank() && artworkUrl.contains("-large.")) {
                        // High quality artwork upgrade
                        artworkUrl = artworkUrl.replace("-large.", "-t500x500.")
                    } else if (artworkUrl.isBlank()) {
                        artworkUrl = userObj?.optString("avatar_url", "") ?: ""
                    }

                    val trackAuth = track.optString("track_authorization", "")
                    val mediaObj = track.optJSONObject("media")
                    val transcodingsArr = mediaObj?.optJSONArray("transcodings")

                    val transcodingsList = mutableListOf<TranscodingInfo>()
                    if (transcodingsArr != null) {
                        for (j in 0 until transcodingsArr.length()) {
                            val tObj = transcodingsArr.getJSONObject(j)
                            val tUrl = tObj.optString("url")
                            val format = tObj.optJSONObject("format")
                            val protocol = format?.optString("protocol") ?: ""
                            val mimeType = format?.optString("mime_type") ?: ""
                            val isPreview = tUrl.contains("/preview/")

                            if (tUrl.isNotBlank()) {
                                transcodingsList.add(TranscodingInfo(tUrl, protocol, mimeType, isPreview))
                            }
                        }
                    }

                    val scSongId = "sc_$rawId"
                    trackCache[scSongId] = CachedTrackInfo(
                        rawId = rawId,
                        title = title,
                        artist = artist,
                        trackAuth = trackAuth,
                        transcodings = transcodingsList
                    )

                    songs.add(
                        Song(
                            id = scSongId,
                            title = title,
                            artist = artist,
                            album = "SoundCloud",
                            durationSec = durationMs / 1000,
                            artworkUrl = artworkUrl
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed searching SoundCloud tracks for: $query", e)
        }
        return songs
    }

    /**
     * Resolves the direct playable stream URL for a given track.
     * Takes either an "sc_<id>" ID or raw numeric ID.
     */
    fun resolveStreamUrl(songId: String, title: String = "", artist: String = ""): String {
        val now = System.currentTimeMillis()
        // Check cache (valid for 10 minutes)
        resolvedStreamCache[songId]?.let { (cachedUrl, time) ->
            if (now - time < 10 * 60 * 1000L && cachedUrl.isNotBlank()) {
                return cachedUrl
            }
        }

        val rawId = songId.removePrefix("sc_")
        val cachedInfo = trackCache[songId] ?: trackCache["sc_$rawId"]

        var streamUrl = ""

        if (cachedInfo != null && cachedInfo.transcodings.isNotEmpty()) {
            streamUrl = resolveFromTranscodings(cachedInfo.transcodings, cachedInfo.trackAuth)
        }

        // If not cached, fetch track by ID directly from SoundCloud API
        if (streamUrl.isBlank() && rawId.all { it.isDigit() }) {
            streamUrl = fetchStreamByTrackId(rawId)
        }

        // If still blank, search by title + artist
        if (streamUrl.isBlank() && (title.isNotBlank() || artist.isNotBlank())) {
            streamUrl = resolveStreamBySearch(title, artist)
        }

        if (streamUrl.isNotBlank()) {
            resolvedStreamCache[songId] = Pair(streamUrl, now)
        }
        return streamUrl
    }

    private fun fetchStreamByTrackId(rawId: String): String {
        try {
            val clientId = getClientId()
            val trackUrl = "$API_BASE/tracks/$rawId?client_id=$clientId"
            val (code, body) = executeGet(trackUrl)
            if (code == 200 && !body.isNullOrBlank()) {
                val track = JSONObject(body)
                val trackAuth = track.optString("track_authorization", "")
                val media = track.optJSONObject("media")
                val transcodingsArr = media?.optJSONArray("transcodings") ?: return ""

                val transcodingsList = mutableListOf<TranscodingInfo>()
                for (i in 0 until transcodingsArr.length()) {
                    val tObj = transcodingsArr.getJSONObject(i)
                    val tUrl = tObj.optString("url")
                    val format = tObj.optJSONObject("format")
                    val protocol = format?.optString("protocol") ?: ""
                    val mimeType = format?.optString("mime_type") ?: ""
                    val isPreview = tUrl.contains("/preview/")
                    if (tUrl.isNotBlank()) {
                        transcodingsList.add(TranscodingInfo(tUrl, protocol, mimeType, isPreview))
                    }
                }
                return resolveFromTranscodings(transcodingsList, trackAuth)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching track by ID $rawId", e)
        }
        return ""
    }

    private fun resolveFromTranscodings(transcodings: List<TranscodingInfo>, trackAuth: String): String {
        val clientId = getClientId()

        // Selection priority:
        // 1. Full progressive stream (direct MP3/AAC)
        // 2. Full HLS stream (m3u8 playlist)
        // 3. Preview progressive stream
        // 4. Preview HLS stream
        val progressiveFull = transcodings.firstOrNull { !it.isPreview && it.protocol == "progressive" }
        val hlsFull = transcodings.firstOrNull { !it.isPreview && it.protocol == "hls" }
        val progressivePreview = transcodings.firstOrNull { it.isPreview && it.protocol == "progressive" }
        val hlsPreview = transcodings.firstOrNull { it.isPreview && it.protocol == "hls" }

        val candidate = progressiveFull ?: hlsFull ?: progressivePreview ?: hlsPreview ?: return ""

        var authUrl = "${candidate.url}?client_id=$clientId"
        if (trackAuth.isNotBlank()) {
            authUrl += "&track_authorization=$trackAuth"
        }

        val (code, body) = executeGet(authUrl)
        if (code == 200 && !body.isNullOrBlank()) {
            val json = JSONObject(body)
            val streamUrl = json.optString("url", "")
            if (streamUrl.isNotBlank()) {
                return streamUrl
            }
        }

        // If primary candidate failed, try HLS full if we didn't pick it
        if (candidate != hlsFull && hlsFull != null) {
            var hlsAuthUrl = "${hlsFull.url}?client_id=$clientId"
            if (trackAuth.isNotBlank()) {
                hlsAuthUrl += "&track_authorization=$trackAuth"
            }
            val (hlsCode, hlsBody) = executeGet(hlsAuthUrl)
            if (hlsCode == 200 && !hlsBody.isNullOrBlank()) {
                val json = JSONObject(hlsBody)
                val streamUrl = json.optString("url", "")
                if (streamUrl.isNotBlank()) {
                    return streamUrl
                }
            }
        }

        return ""
    }

    private fun resolveStreamBySearch(title: String, artist: String): String {
        val queries = listOf(
            "$title $artist".trim(),
            title.trim()
        ).distinct()

        for (q in queries) {
            val songs = searchTracks(q, limit = 5)
            for (song in songs) {
                val stream = resolveStreamUrl(song.id, song.title, song.artist)
                if (stream.isNotBlank()) {
                    return stream
                }
            }
        }
        return ""
    }

    /**
     * Checks health and latency of the SoundCloud API provider.
     * Uses dynamic client_id and auto-retry to avoid false 401s.
     */
    fun checkHealth(): Pair<Boolean, Long> {
        val start = System.currentTimeMillis()
        return try {
            val clientId = getClientId()
            val url = "$API_BASE/search/tracks?q=starboy&client_id=$clientId&limit=1"
            val (code, _) = executeGet(url)
            val latency = System.currentTimeMillis() - start
            Pair(code in 200..299, latency)
        } catch (e: Exception) {
            Pair(false, 0L)
        }
    }
}
