package com.example.data.repository

import com.example.domain.model.*
import dev.toastbits.ytmkt.model.YtmApi
import dev.toastbits.ytmkt.model.external.mediaitem.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import dev.toastbits.ytmkt.endpoint.SearchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

fun getTrendingSongsFlow(api: YtmApi): Flow<List<Song>> = flow {
    var songs = mutableListOf<Song>()
    try {
        try {
            val feed = api.SongFeed.getSongFeed().getOrNull()
            feed?.layouts?.forEach { layout ->
                songs.addAll(layout.items.filterIsInstance<YtmSong>().map { 
                    Song(
                        id = it.id,
                        title = it.name ?: "Unknown Title",
                        artist = it.artists?.firstOrNull()?.name ?: "Unknown Artist",
                        artistId = it.artists?.firstOrNull()?.id ?: "",
                        album = it.album?.name ?: "",
                        albumId = it.album?.id ?: "",
                        durationSec = (it.duration ?: 0).toInt(),
                        artworkUrl = it.thumbnail_provider?.getThumbnailUrl(dev.toastbits.ytmkt.model.external.ThumbnailProvider.Quality.HIGH) ?: ""
                    )
                })
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }

        if (songs.isEmpty()) {
            try {
                val res = api.Search.search("Top Hits", SearchType.SONG.getDefaultParams()).getOrNull()
                res?.categories?.firstOrNull()?.first?.items?.filterIsInstance<YtmSong>()?.map {
                    Song(
                        id = it.id,
                        title = it.name ?: "Unknown Title",
                        artist = it.artists?.firstOrNull()?.name ?: "Unknown Artist",
                        artistId = it.artists?.firstOrNull()?.id ?: "",
                        album = it.album?.name ?: "",
                        albumId = it.album?.id ?: "",
                        durationSec = (it.duration ?: 0).toInt(),
                        artworkUrl = it.thumbnail_provider?.getThumbnailUrl(dev.toastbits.ytmkt.model.external.ThumbnailProvider.Quality.HIGH) ?: ""
                    )
                }?.let { songs.addAll(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // JioSaavn Charts & Trending Fallback
        if (songs.isEmpty()) {
            val jioSongs = fetchJioSaavnChartSongs()
            songs.addAll(jioSongs)
        }
        
        if (songs.isEmpty()) {
            throw Exception("Unable to load trending tracks at this time. Please use Search.")
        }
        
        emit(songs)
    } catch (e: Exception) {
        throw Exception("Failed to load trending: ${e.message}")
    }
}

fun getFeaturedAlbumsFlow(api: YtmApi): Flow<List<Album>> = flow {
    try {
        var albums = emptyList<Album>()
        try {
            val res = api.Search.search("New Albums", SearchType.ALBUM.getDefaultParams()).getOrNull()
            albums = res?.categories?.firstOrNull()?.first?.items?.filterIsInstance<YtmPlaylist>()?.map {
                Album(
                    id = it.id,
                    title = it.name ?: "Unknown Album",
                    artist = "Various Artists",
                    artworkUrl = it.thumbnail_provider?.getThumbnailUrl(dev.toastbits.ytmkt.model.external.ThumbnailProvider.Quality.HIGH) ?: "",
                    trackCount = it.items?.size ?: 0
                )
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (albums.isEmpty()) {
            albums = fetchJioSaavnNewAlbums()
        }
        
        emit(albums)
    } catch (e: Exception) {
        emit(emptyList())
    }
}

fun getFeaturedPlaylistsFlow(api: YtmApi): Flow<List<Playlist>> = flow {
    try {
        var playlists = emptyList<Playlist>()
        try {
            val res = api.Search.search("Trending Playlists", SearchType.PLAYLIST.getDefaultParams()).getOrNull()
            playlists = res?.categories?.firstOrNull()?.first?.items?.filterIsInstance<YtmPlaylist>()?.map {
                Playlist(
                    id = it.id,
                    title = it.name ?: "Playlist",
                    artworkUrl = it.thumbnail_provider?.getThumbnailUrl(dev.toastbits.ytmkt.model.external.ThumbnailProvider.Quality.HIGH) ?: "",
                    trackCount = it.items?.size ?: 0
                )
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (playlists.isEmpty()) {
            playlists = fetchJioSaavnTopPlaylists()
        }
        
        emit(playlists)
    } catch (e: Exception) {
        emit(emptyList())
    }
}

private fun cleanHtml(text: String): String {
    return text.replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&#039;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}

private fun fetchJioSaavnChartSongs(): List<Song> {
    try {
        val launchUrl = URL("https://www.jiosaavn.com/api.php?__call=webapi.getLaunchData&api_version=4&_format=json&_marker=0&ctx=web6dot0")
        val conn = launchUrl.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        if (conn.responseCode == 200) {
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val charts = json.optJSONArray("charts")
            if (charts != null && charts.length() > 0) {
                val chartId = charts.getJSONObject(0).optString("id")
                if (chartId.isNotBlank()) {
                    val pUrl = URL("https://www.jiosaavn.com/api.php?__call=playlist.getDetails&listid=$chartId&_format=json&_marker=0&ctx=web6dot0")
                    val pConn = pUrl.openConnection() as HttpURLConnection
                    pConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    pConn.connectTimeout = 4000
                    pConn.readTimeout = 4000
                    if (pConn.responseCode == 200) {
                        val pBody = pConn.inputStream.bufferedReader().readText()
                        val pJson = JSONObject(pBody)
                        val songArr = pJson.optJSONArray("songs")
                        if (songArr != null && songArr.length() > 0) {
                            val list = mutableListOf<Song>()
                            for (i in 0 until songArr.length()) {
                                val s = songArr.getJSONObject(i)
                                val id = s.optString("id")
                                val title = cleanHtml(s.optString("song"))
                                val artist = cleanHtml(s.optString("primary_artists"))
                                val album = cleanHtml(s.optString("album"))
                                val img = s.optString("image").replace("150x150", "500x500")
                                val dur = s.optInt("duration", 0)
                                if (id.isNotBlank() && title.isNotBlank()) {
                                    list.add(
                                        Song(
                                            id = id,
                                            title = title,
                                            artist = if (artist.isNotBlank()) artist else "Popular Artist",
                                            album = album,
                                            durationSec = dur,
                                            artworkUrl = img
                                        )
                                    )
                                }
                            }
                            return list
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList()
}

private fun fetchJioSaavnNewAlbums(): List<Album> {
    try {
        val launchUrl = URL("https://www.jiosaavn.com/api.php?__call=webapi.getLaunchData&api_version=4&_format=json&_marker=0&ctx=web6dot0")
        val conn = launchUrl.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        if (conn.responseCode == 200) {
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val albumsArr = json.optJSONArray("new_albums")
            if (albumsArr != null && albumsArr.length() > 0) {
                val list = mutableListOf<Album>()
                for (i in 0 until albumsArr.length()) {
                    val a = albumsArr.getJSONObject(i)
                    val id = a.optString("id")
                    val title = cleanHtml(a.optString("title"))
                    val img = a.optString("image").replace("150x150", "500x500")
                    if (id.isNotBlank() && title.isNotBlank()) {
                        list.add(
                            Album(
                                id = id,
                                title = title,
                                artist = "Featured Release",
                                artworkUrl = img,
                                trackCount = 0
                            )
                        )
                    }
                }
                return list
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList()
}

private fun fetchJioSaavnTopPlaylists(): List<Playlist> {
    try {
        val launchUrl = URL("https://www.jiosaavn.com/api.php?__call=webapi.getLaunchData&api_version=4&_format=json&_marker=0&ctx=web6dot0")
        val conn = launchUrl.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        if (conn.responseCode == 200) {
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val pArr = json.optJSONArray("top_playlists")
            if (pArr != null && pArr.length() > 0) {
                val list = mutableListOf<Playlist>()
                for (i in 0 until pArr.length()) {
                    val p = pArr.getJSONObject(i)
                    val id = p.optString("id")
                    val title = cleanHtml(p.optString("title"))
                    val img = p.optString("image").replace("150x150", "500x500")
                    if (id.isNotBlank() && title.isNotBlank()) {
                        list.add(
                            Playlist(
                                id = id,
                                title = title,
                                artworkUrl = img,
                                trackCount = 0
                            )
                        )
                    }
                }
                return list
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList()
}

