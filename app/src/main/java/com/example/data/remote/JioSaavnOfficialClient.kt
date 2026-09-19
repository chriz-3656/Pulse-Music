package com.example.data.remote

import android.util.Log
import com.example.domain.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object JioSaavnOfficialClient {
    private const val TAG = "JioSaavnOfficialClient"
    private const val BASE_URL = "https://www.jiosaavn.com/api.php?_format=json&_marker=0&ctx=android"

    private fun executeGet(urlString: String): Pair<Int, String?> {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val code = conn.responseCode
            if (code == 200) {
                val body = InputStreamReader(conn.inputStream).readText()
                Pair(code, body)
            } else {
                Pair(code, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request failed: $urlString", e)
            Pair(500, null)
        }
    }

    fun searchSongs(query: String, limit: Int = 20): List<Song> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "$BASE_URL&__call=search.getResults&q=$encoded&p=1&n=$limit"
        val songs = mutableListOf<Song>()
        
        val (code, body) = executeGet(url)
        if (code == 200 && !body.isNullOrBlank()) {
            try {
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return emptyList()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    songs.add(parseSongObject(item))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing search results", e)
            }
        }
        return songs
    }

    fun getRecommendations(songId: String): List<Song> {
        val url = "$BASE_URL&__call=reco.getreco&pid=$songId"
        val songs = mutableListOf<Song>()
        
        val (code, body) = executeGet(url)
        if (code == 200 && !body.isNullOrBlank()) {
            try {
                // reco returns a json array directly
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    songs.add(parseSongObject(item))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing recommendations", e)
            }
        }
        return songs
    }

    fun getLyrics(songId: String): String {
        val url = "$BASE_URL&__call=lyrics.getLyrics&lyrics_id=$songId"
        val (code, body) = executeGet(url)
        if (code == 200 && !body.isNullOrBlank()) {
            try {
                val json = JSONObject(body)
                return json.optString("lyrics", "").replace("<br>", "\n")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing lyrics", e)
            }
        }
        return ""
    }
    
    fun getSongDetails(songId: String): Song? {
        val url = "$BASE_URL&__call=song.getDetails&pids=$songId"
        val (code, body) = executeGet(url)
        if (code == 200 && !body.isNullOrBlank()) {
            try {
                val json = JSONObject(body)
                val songsMap = json.optJSONObject("songs")
                if (songsMap != null) {
                    val songObj = songsMap.optJSONObject(songId)
                        ?: if (songsMap.keys().hasNext()) songsMap.optJSONObject(songsMap.keys().next()) else null
                    if (songObj != null) {
                        return parseSongObject(songObj)
                    }
                } else if (json.has(songId)) {
                    return parseSongObject(json.optJSONObject(songId)!!)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing song details", e)
            }
        }
        return null
    }

    private fun parseSongObject(item: JSONObject): Song {
        val id = item.optString("id")
        var title = item.optString("song")
        if (title.isBlank()) title = item.optString("title", "Unknown")
        var artist = item.optString("primary_artists")
        if (artist.isBlank()) artist = item.optString("singers", "Unknown")
        val album = item.optString("album", "Unknown")
        val durationSec = item.optString("duration", "0").toLongOrNull()?.toInt() ?: 0
        var imageUrl = item.optString("image", "")
        if (imageUrl.isNotBlank()) {
            imageUrl = imageUrl.replace("150x150", "500x500").replace("50x50", "500x500")
        }
        
        val moreInfo = item.optJSONObject("more_info")
        var encryptedMediaUrl = item.optString("encrypted_media_url", "")
        if (encryptedMediaUrl.isBlank() && moreInfo != null) {
            encryptedMediaUrl = moreInfo.optString("encrypted_media_url", "")
        }

        return Song(
            id = id,
            title = title.replace("&quot;", "\""),
            artist = artist.replace("&quot;", "\""),
            album = album.replace("&quot;", "\""),
            durationSec = durationSec,
            artworkUrl = imageUrl,
            stream320Url = encryptedMediaUrl // we'll decrypt this in repository
        )
    }
}
