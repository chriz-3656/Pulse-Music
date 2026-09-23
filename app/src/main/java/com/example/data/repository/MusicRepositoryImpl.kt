package com.example.data.repository

import android.content.Context
import com.example.data.local.dao.*
import com.example.data.player.MediaCacheManager
import com.example.data.player.TrackDownloadManager
import com.example.data.remote.MusicApiService
import com.example.data.remote.SoundCloudClient
import com.example.domain.model.*
import com.example.domain.repository.MusicRepository
import com.example.domain.repository.SearchResults
import com.squareup.moshi.Moshi
import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.YtmApi
import dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import dev.toastbits.ytmkt.model.external.ThumbnailProvider
import dev.toastbits.ytmkt.endpoint.SearchType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import java.net.URL
import java.net.HttpURLConnection
import java.net.URLEncoder
import org.json.JSONObject
import org.json.JSONArray

class MusicRepositoryImpl(
    private val context: Context,
    private val apiService: MusicApiService, // keep for DI signature but ignore
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val recentSearchDao: RecentSearchDao,
    private val mediaCacheManager: MediaCacheManager,
    private val downloadManager: TrackDownloadManager,
    private val moshi: Moshi
) : MusicRepository {

    private val prefs = context.getSharedPreferences("pulse_music_settings", Context.MODE_PRIVATE)
    private val _userSettingsFlow = MutableStateFlow(loadSettingsFromPrefs())

    private fun loadSettingsFromPrefs(): UserSettings {
        val qualityStr = prefs.getString("audio_quality", AudioQuality.HIGH.name) ?: AudioQuality.HIGH.name
        val quality = try { AudioQuality.valueOf(qualityStr) } catch (_: Exception) { AudioQuality.HIGH }
        return UserSettings(
            audioQuality = quality,
            cacheSizeLimitMb = prefs.getInt("cache_size_limit_mb", 1000),
            crossfadeDurationSec = prefs.getInt("crossfade_sec", 2),
            isDarkTheme = prefs.getBoolean("is_dark_theme", true),
            autoSkipFailedTracks = prefs.getBoolean("auto_skip_failed", true),
            autoplayEnabled = prefs.getBoolean("autoplay_enabled", true),
            customApiBaseUrl = prefs.getString("custom_api_url", "") ?: "",
            apiProvider = prefs.getString("api_provider", "auto") ?: "auto"
        )
    }

    private val api: YtmApi = YoutubeiApi(data_language = "en-GB")
    private val songCache = java.util.concurrent.ConcurrentHashMap<String, Song>()

    private fun YtmSong.toDomain(streamUrl: String = ""): Song {
        val durationStr = this.duration?.toString() ?: "0"
        val durationInt = if (durationStr.contains(":")) {
            val parts = durationStr.split(":")
            if (parts.size == 2) {
                (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
            } else if (parts.size == 3) {
                (parts[0].toIntOrNull() ?: 0) * 3600 + (parts[1].toIntOrNull() ?: 0) * 60 + (parts[2].toIntOrNull() ?: 0)
            } else 0
        } else {
            durationStr.toIntOrNull() ?: 0
        }
        val artwork = this.thumbnail_provider?.getThumbnailUrl(ThumbnailProvider.Quality.HIGH) ?: ""
        val artistName = this.artists?.firstOrNull()?.name ?: "Unknown Artist"
        val artistId = this.artists?.firstOrNull()?.id ?: ""
        
        return Song(
            id = this.id,
            title = this.name ?: "Unknown Title",
            artist = artistName,
            artistId = artistId,
            album = this.album?.name ?: "",
            albumId = this.album?.id ?: "",
            durationSec = durationInt,
            artworkUrl = artwork,
            stream160Url = streamUrl,
            stream320Url = streamUrl
        )
    }

    private fun YtmArtist.toDomain(): Artist {
        return Artist(
            id = this.id,
            name = this.name ?: "Unknown Artist",
            imageUrl = this.thumbnail_provider?.getThumbnailUrl(ThumbnailProvider.Quality.HIGH) ?: ""
        )
    }

    private fun YtmPlaylist.toDomainAlbum(): Album {
        return Album(
            id = this.id,
            title = this.name ?: "Unknown Album",
            artist = "Various Artists", // ytmkt playlist doesn't expose artist list directly here easily
            artworkUrl = this.thumbnail_provider?.getThumbnailUrl(ThumbnailProvider.Quality.HIGH) ?: "",
            trackCount = this.items?.size ?: 0
        )
    }
    
    private fun YtmPlaylist.toDomainPlaylist(): Playlist {
        return Playlist(
            id = this.id,
            title = this.name ?: "Playlist",
            artworkUrl = this.thumbnail_provider?.getThumbnailUrl(ThumbnailProvider.Quality.HIGH) ?: "",
            trackCount = this.items?.size ?: 0
        )
    }

    override suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val provider = _userSettingsFlow.value.provider
        when (provider) {
            MusicProvider.SOUNDCLOUD -> {
                val sc = searchSoundCloudSongs(query)
                if (sc.isNotEmpty()) return@withContext sc
            }
            MusicProvider.JIOSAAVN -> {
                val jio = searchJioSaavnSongs(query)
                if (jio.isNotEmpty()) return@withContext jio
            }
            MusicProvider.YOUTUBE -> {
                try {
                    val res = api.Search.search(query, SearchType.SONG.getDefaultParams()).getOrNull()
                    val ytmSongs = res?.categories?.firstOrNull()?.first?.items?.filterIsInstance<YtmSong>()?.map { it.toDomain() } ?: emptyList()
                    if (ytmSongs.isNotEmpty()) return@withContext ytmSongs
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            MusicProvider.AUTO -> {
                try {
                    val res = api.Search.search(query, SearchType.SONG.getDefaultParams()).getOrNull()
                    val ytmSongs = res?.categories?.firstOrNull()?.first?.items?.filterIsInstance<YtmSong>()?.map { it.toDomain() } ?: emptyList()
                    if (ytmSongs.isNotEmpty()) return@withContext ytmSongs
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val jioSongs = searchJioSaavnSongs(query)
                if (jioSongs.isNotEmpty()) return@withContext jioSongs
                return@withContext searchSoundCloudSongs(query)
            }
        }
        searchJioSaavnSongs(query)
    }


    override suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val res = api.SearchSuggestions.getSearchSuggestions(query).getOrNull()
            val list = res?.map { it.text } ?: emptyList()
            if (list.isNotEmpty()) return@withContext list
        } catch (e: Exception) {
            e.printStackTrace()
        }
        fetchJioSaavnSuggestions(query)
    }

    override suspend fun searchAll(query: String): SearchResults = withContext(Dispatchers.IO) {
        val provider = _userSettingsFlow.value.provider

        if (provider == MusicProvider.SOUNDCLOUD) {
            val scSongs = searchSoundCloudSongs(query)
            if (scSongs.isNotEmpty()) {
                return@withContext SearchResults(songs = scSongs.take(30))
            }
        } else if (provider == MusicProvider.JIOSAAVN) {
            val jioResults = searchJioSaavnAndSoundCloudAll(query)
            if (jioResults.songs.isNotEmpty() || jioResults.albums.isNotEmpty() || jioResults.artists.isNotEmpty()) {
                return@withContext jioResults
            }
        }

        var ytmResults: SearchResults? = null
        
        try {
            val songsDeferred = async { api.Search.search(query, params = dev.toastbits.ytmkt.endpoint.SearchType.SONG.getDefaultParams()).getOrNull() }
            val albumsDeferred = async { api.Search.search(query, params = dev.toastbits.ytmkt.endpoint.SearchType.ALBUM.getDefaultParams()).getOrNull() }
            val artistsDeferred = async { api.Search.search(query, params = dev.toastbits.ytmkt.endpoint.SearchType.ARTIST.getDefaultParams()).getOrNull() }
            val playlistsDeferred = async { api.Search.search(query, params = dev.toastbits.ytmkt.endpoint.SearchType.PLAYLIST.getDefaultParams()).getOrNull() }

            val songItems = songsDeferred.await()?.categories?.firstOrNull()?.first?.items?.filterIsInstance<dev.toastbits.ytmkt.model.external.mediaitem.YtmSong>()?.map { it.toDomain() } ?: emptyList()
            val albumItems = albumsDeferred.await()?.categories?.firstOrNull()?.first?.items?.filterIsInstance<dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist>()?.map { it.toDomainAlbum() } ?: emptyList()
            val artistItems = artistsDeferred.await()?.categories?.firstOrNull()?.first?.items?.filterIsInstance<dev.toastbits.ytmkt.model.external.mediaitem.YtmArtist>()?.map { it.toDomain() } ?: emptyList()
            val playlistItems = playlistsDeferred.await()?.categories?.firstOrNull()?.first?.items?.filterIsInstance<dev.toastbits.ytmkt.model.external.mediaitem.YtmPlaylist>()?.map { it.toDomainPlaylist() } ?: emptyList()

            ytmResults = SearchResults(
                songs = songItems,
                albums = albumItems,
                artists = artistItems,
                playlists = playlistItems
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (ytmResults != null && ytmResults.songs.isNotEmpty()) {
            return@withContext ytmResults
        }

        searchJioSaavnAndSoundCloudAll(query)
    }

    override fun getRecentSearches(): Flow<List<String>> = recentSearchDao.getRecentSearches().map { list -> list.map { it.query } }
    override suspend fun saveSearchQuery(query: String) = recentSearchDao.insertSearch(com.example.data.local.entity.RecentSearchEntity(query))
    override suspend fun deleteSearchQuery(query: String) = recentSearchDao.deleteSearch(query)
    override suspend fun clearSearchHistory() = recentSearchDao.clearAll()

    override suspend fun getSongDetails(id: String): Result<Song> = withContext(Dispatchers.IO) {
        try {
            // Check local DB first
            val local = songDao.getSongById(id)
            if (local != null) {
                var streamUrl = if (local.stream320Url.isNotBlank()) local.stream320Url else local.stream160Url
                if (streamUrl.isBlank()) {
                    streamUrl = fetchStreamUrl(id, local.title, local.artist)
                }
                return@withContext Result.success(
                    Song(
                        id = local.id,
                        title = local.title,
                        artist = local.artist,
                        album = local.album,
                        durationSec = local.durationSec,
                        artworkUrl = local.artworkUrl,
                        stream160Url = streamUrl,
                        stream320Url = streamUrl,
                        lyrics = local.lyrics,
                        isDownloaded = local.isDownloaded,
                        isFavorite = local.isFavorite,
                        localFilePath = local.localFilePath,
                        year = local.year,
                        artistId = local.artistId,
                        albumId = local.albumId
                    )
                )
            }

            // SoundCloud track direct retrieval
            if (id.startsWith("sc_")) {
                val cached = songCache[id]
                val stream = SoundCloudClient.resolveStreamUrl(id, cached?.title ?: "", cached?.artist ?: "")
                if (cached != null) {
                    val updated = cached.copy(stream160Url = stream, stream320Url = stream)
                    songCache[id] = updated
                    return@withContext Result.success(updated)
                }
            }

            // Try YTM
            val songRes = api.LoadSong.loadSong(id).getOrNull()
            if (songRes != null) {
                val title = songRes.name ?: ""
                val artist = songRes.artists?.firstOrNull()?.name ?: ""
                val streamUrl = fetchStreamUrl(id, title, artist)
                return@withContext Result.success(songRes.toDomain(streamUrl))
            }

            // Fallback: JioSaavn song.getDetails by ID
            val jioSong = fetchJioSaavnSongDetails(id)
            if (jioSong != null) {
                return@withContext Result.success(jioSong)
            }

            Result.failure(Exception("Song not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resolveStreamUrl(song: Song): String = withContext(Dispatchers.IO) {
        songCache[song.id] = song
        val existing = song.getStreamUrl(preferHighQuality = true)
        if (existing.isNotBlank()) return@withContext existing

        // Direct SoundCloud stream extraction if song is from SoundCloud
        if (song.id.startsWith("sc_")) {
            val scUrl = SoundCloudClient.resolveStreamUrl(song.id, song.title, song.artist)
            if (scUrl.isNotBlank()) {
                val updated = song.copy(stream160Url = scUrl, stream320Url = scUrl)
                songCache[song.id] = updated
                return@withContext scUrl
            }
        }

        val fetched = fetchStreamUrl(song.id, song.title, song.artist)
        if (fetched.isNotBlank()) {
            songCache[song.id] = song.copy(stream160Url = fetched, stream320Url = fetched)
        }
        fetched
    }

    override suspend fun getSongsBatch(ids: List<String>): List<Song> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Song>()
        for (id in ids) {
            getSongDetails(id).getOrNull()?.let { list.add(it) }
        }
        list
    }

    override suspend fun getLyrics(id: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val lyricsBrowseId = api.LoadSong.loadSong(id).getOrNull()?.lyrics_browse_id
            if (lyricsBrowseId != null) {
                val lyricsResult = api.SongLyrics.getSongLyrics(lyricsBrowseId).getOrNull()
                if (lyricsResult != null && lyricsResult.isNotBlank()) {
                    return@withContext Result.success(lyricsResult)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check memory cache first, then local DB
        val cached = songCache[id]
        val localSong = songDao.getSongById(id)
        val title = cached?.title ?: localSong?.title ?: ""
        val artist = cached?.artist ?: localSong?.artist ?: ""

        if (title.isNotBlank()) {
            val lrcLyrics = fetchLrclibLyrics(title, artist)
            if (lrcLyrics.isNotBlank()) {
                return@withContext Result.success(lrcLyrics)
            }
        }

        val jioLyrics = fetchJioSaavnLyrics(id)
        if (jioLyrics.isNotBlank()) {
            return@withContext Result.success(jioLyrics)
        }

        Result.failure(Exception("No lyrics found"))
    }

    override suspend fun getSongSuggestions(song: Song): List<Song> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Song>()
        val seedId = song.id
        val artist = song.artist.trim()
        val title = song.title.trim()
        val provider = _userSettingsFlow.value.provider

        if (provider == MusicProvider.SOUNDCLOUD || seedId.startsWith("sc_")) {
            val scRadio = SoundCloudClient.getRelatedTracks(seedId, 10)
            if (scRadio.isNotEmpty()) list.addAll(scRadio)
        } 
        
        if (list.isEmpty() && (provider == MusicProvider.YOUTUBE || (provider == MusicProvider.AUTO && !seedId.startsWith("sc_")))) {
            try {
                val radio = api.SongRadio.getSongRadio(seedId, null).getOrNull()
                val ytmSongs = radio?.items?.map { it.toDomain() } ?: emptyList()
                if (ytmSongs.isNotEmpty()) list.addAll(ytmSongs)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            if (list.isEmpty() && (artist.isNotBlank() || title.isNotBlank())) {
                try {
                    val searchRes = api.Search.search(if (artist.isNotBlank()) artist else title, dev.toastbits.ytmkt.endpoint.SearchType.SONG.getDefaultParams()).getOrNull()
                    val artistSongs = searchRes?.categories?.firstOrNull()?.first?.items?.filterIsInstance<dev.toastbits.ytmkt.model.external.mediaitem.YtmSong>()?.map { it.toDomain() } ?: emptyList()
                    list.addAll(artistSongs)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (list.isEmpty() && (provider == MusicProvider.JIOSAAVN || provider == MusicProvider.AUTO)) {
            val saavnCleanId = seedId.removePrefix("sc_")
            val jioReco = fetchJioSaavnRadioTracks(saavnCleanId)
            if (jioReco.isNotEmpty()) list.addAll(jioReco)
            
            if (list.isEmpty() && (artist.isNotBlank() || title.isNotBlank())) {
                val query = if (artist.isNotBlank()) artist else title
                list.addAll(searchJioSaavnSongs(query))
            }
        }

        val seen = mutableSetOf<String>()
        val result = mutableListOf<Song>()
        for (item in list) {
            if (item.id != seedId && item.title.isNotBlank() && seen.add(item.id)) {
                result.add(item)
            }
        }
        result.shuffle()
        result
    }

    override suspend fun getSongSuggestions(songId: String): List<Song> = withContext(Dispatchers.IO) {
        val cached = songCache[songId] ?: songDao.getSongById(songId)?.let {
            Song(
                id = it.id,
                title = it.title,
                artist = it.artist,
                album = it.album,
                durationSec = it.durationSec,
                artworkUrl = it.artworkUrl,
                stream160Url = it.stream160Url,
                stream320Url = it.stream320Url,
                artistId = it.artistId,
                albumId = it.albumId
            )
        }
        if (cached != null) {
            return@withContext getSongSuggestions(cached)
        }
        getSongSuggestions(Song(id = songId, title = "", artist = ""))
    }

    override suspend fun getAlbumDetails(id: String): Result<Album> = withContext(Dispatchers.IO) {
        try {
            val playlist = api.LoadPlaylist.loadPlaylist(id).getOrNull()
            if (playlist != null) {
                val songs = playlist.items?.filterIsInstance<YtmSong>()?.map { it.toDomain() } ?: emptyList()
                return@withContext Result.success(playlist.toDomainAlbum().copy(songs = songs))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val jioAlbum = fetchJioSaavnAlbumDetails(id)
        if (jioAlbum != null) {
            return@withContext Result.success(jioAlbum)
        }
        Result.failure(Exception("Album not found"))
    }

    override suspend fun getPlaylistDetails(id: String): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val playlist = api.LoadPlaylist.loadPlaylist(id).getOrNull()
            if (playlist != null) {
                val songs = playlist.items?.filterIsInstance<YtmSong>()?.map { it.toDomain() } ?: emptyList()
                return@withContext Result.success(playlist.toDomainPlaylist().copy(songs = songs))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val jioPlaylist = fetchJioSaavnPlaylistDetails(id)
        if (jioPlaylist != null) {
            return@withContext Result.success(jioPlaylist)
        }
        Result.failure(Exception("Playlist not found"))
    }

    override suspend fun getArtistProfile(id: String): Result<Artist> = withContext(Dispatchers.IO) {
        try {
            val artist = api.LoadArtist.loadArtist(id).getOrNull()
            if (artist != null) {
                return@withContext Result.success(artist.toDomain())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Result.failure(Exception("Artist not found"))
    }

    override suspend fun getArtistSongs(artistId: String): List<Song> = withContext(Dispatchers.IO) {
        emptyList()
    }

    override suspend fun getArtistAlbums(artistId: String): List<Album> = withContext(Dispatchers.IO) {
        emptyList()
    }

    private fun searchJioSaavnSongs(query: String): List<Song> {
        val list = mutableListOf<Song>()
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = URL("https://www.jiosaavn.com/api.php?__call=search.getResults&q=$encoded&p=1&n=25&_format=json&_marker=0&ctx=web6dot0")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                val results = json.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val s = results.getJSONObject(i)
                        val id = s.optString("id")
                        val title = cleanSaavnText(s.optString("song"))
                        val artist = cleanSaavnText(s.optString("primary_artists"))
                        val album = cleanSaavnText(s.optString("album"))
                        val dur = s.optInt("duration", 0)
                        val img = s.optString("image").replace("150x150", "500x500")
                        val encMedia = s.optString("encrypted_media_url")
                        var streamUrl = ""
                        if (encMedia.isNotBlank()) {
                            val dec = decryptJioSaavnUrl(encMedia)
                            if (dec.startsWith("http")) {
                                streamUrl = dec.replace("_96.mp4", "_320.mp4").replace("_96.m4a", "_320.mp4")
                            }
                        }
                        if (id.isNotBlank() && title.isNotBlank()) {
                            list.add(
                                Song(
                                    id = id,
                                    title = title,
                                    artist = if (artist.isNotBlank()) artist else "Popular Artist",
                                    album = album,
                                    durationSec = dur,
                                    artworkUrl = img,
                                    stream160Url = streamUrl,
                                    stream320Url = streamUrl
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun searchSoundCloudSongs(query: String): List<Song> {
        return SoundCloudClient.searchTracks(query, limit = 20)
    }

    private fun fetchJioSaavnSuggestions(query: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = URL("https://www.jiosaavn.com/api.php?__call=autocomplete.get&query=$encoded&_format=json&_marker=0&ctx=android")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                val songsObj = json.optJSONObject("songs")
                val songsData = songsObj?.optJSONArray("data")
                if (songsData != null) {
                    for (i in 0 until minOf(songsData.length(), 6)) {
                        val title = cleanSaavnText(songsData.getJSONObject(i).optString("title"))
                        if (title.isNotBlank() && !list.contains(title)) {
                            list.add(title)
                        }
                    }
                }
                val topObj = json.optJSONObject("topquery")
                val topData = topObj?.optJSONArray("data")
                if (topData != null) {
                    for (i in 0 until minOf(topData.length(), 4)) {
                        val title = cleanSaavnText(topData.getJSONObject(i).optString("title"))
                        if (title.isNotBlank() && !list.contains(title)) {
                            list.add(title)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun searchJioSaavnAndSoundCloudAll(query: String): SearchResults {
        val songs = mutableListOf<Song>()
        val albums = mutableListOf<Album>()
        val playlists = mutableListOf<Playlist>()
        val artists = mutableListOf<Artist>()

        // 1. Search JioSaavn Songs
        songs.addAll(searchJioSaavnSongs(query))

        // 2. Search SoundCloud Songs
        if (songs.size < 10) {
            val scSongs = searchSoundCloudSongs(query)
            for (sc in scSongs) {
                if (songs.none { it.title.equals(sc.title, ignoreCase = true) }) {
                    songs.add(sc)
                }
            }
        }

        // 3. Search JioSaavn Autocomplete for Albums, Artists, Playlists
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val autoUrl = URL("https://www.jiosaavn.com/api.php?__call=autocomplete.get&query=$encoded&_format=json&_marker=0&ctx=android")
            val conn = autoUrl.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                val albObj = json.optJSONObject("albums")
                val albData = albObj?.optJSONArray("data")
                if (albData != null) {
                    for (i in 0 until albData.length()) {
                        val a = albData.getJSONObject(i)
                        val id = a.optString("id")
                        val title = cleanSaavnText(a.optString("title"))
                        val img = a.optString("image").replace("150x150", "500x500")
                        val desc = cleanSaavnText(a.optString("description"))
                        if (id.isNotBlank() && title.isNotBlank()) {
                            albums.add(Album(id = id, title = title, artist = desc, artworkUrl = img, trackCount = 0))
                        }
                    }
                }
                val artObj = json.optJSONObject("artists")
                val artData = artObj?.optJSONArray("data")
                if (artData != null) {
                    for (i in 0 until artData.length()) {
                        val a = artData.getJSONObject(i)
                        val id = a.optString("id")
                        val name = cleanSaavnText(a.optString("title"))
                        val img = a.optString("image").replace("150x150", "500x500")
                        if (id.isNotBlank() && name.isNotBlank()) {
                            artists.add(Artist(id = id, name = name, imageUrl = img))
                        }
                    }
                }
                val playObj = json.optJSONObject("playlists")
                val playData = playObj?.optJSONArray("data")
                if (playData != null) {
                    for (i in 0 until playData.length()) {
                        val p = playData.getJSONObject(i)
                        val id = p.optString("id")
                        val title = cleanSaavnText(p.optString("title"))
                        val img = p.optString("image").replace("150x150", "500x500")
                        if (id.isNotBlank() && title.isNotBlank()) {
                            playlists.add(Playlist(id = id, title = title, artworkUrl = img, trackCount = 0))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return SearchResults(
            songs = songs.take(30),
            albums = albums.take(15),
            playlists = playlists.take(15),
            artists = artists.take(15)
        )
    }

    private fun fetchJioSaavnSongDetails(id: String): Song? {
        try {
            val url = URL("https://www.jiosaavn.com/api.php?__call=song.getDetails&pids=$id&_format=json&_marker=0&ctx=android")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                val songObj = json.optJSONObject(id)
                if (songObj != null) {
                    val title = cleanSaavnText(songObj.optString("song"))
                    val artist = cleanSaavnText(songObj.optString("primary_artists"))
                    val album = cleanSaavnText(songObj.optString("album"))
                    val dur = songObj.optInt("duration", 0)
                    val img = songObj.optString("image").replace("150x150", "500x500")
                    val encMedia = songObj.optString("encrypted_media_url")
                    var streamUrl = ""
                    if (encMedia.isNotBlank()) {
                        val dec = decryptJioSaavnUrl(encMedia)
                        if (dec.startsWith("http")) {
                            streamUrl = dec.replace("_96.mp4", "_320.mp4").replace("_96.m4a", "_320.mp4")
                        }
                    }
                    return Song(
                        id = id,
                        title = title,
                        artist = if (artist.isNotBlank()) artist else "Popular Artist",
                        album = album,
                        durationSec = dur,
                        artworkUrl = img,
                        stream160Url = streamUrl,
                        stream320Url = streamUrl
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun fetchJioSaavnAlbumDetails(id: String): Album? {
        try {
            val url = URL("https://www.jiosaavn.com/api.php?__call=content.getAlbumDetails&albumid=$id&_format=json&_marker=0&ctx=web6dot0")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                val title = cleanSaavnText(json.optString("title"))
                val artist = cleanSaavnText(json.optString("primary_artists"))
                val img = json.optString("image").replace("150x150", "500x500")
                val songsArr = json.optJSONArray("songs")
                val songList = mutableListOf<Song>()
                if (songsArr != null) {
                    for (i in 0 until songsArr.length()) {
                        val s = songsArr.getJSONObject(i)
                        val sId = s.optString("id")
                        val sTitle = cleanSaavnText(s.optString("song"))
                        val sArtist = cleanSaavnText(s.optString("primary_artists"))
                        val sDur = s.optInt("duration", 0)
                        val sImg = s.optString("image").replace("150x150", "500x500")
                        val encMedia = s.optString("encrypted_media_url")
                        var sStream = ""
                        if (encMedia.isNotBlank()) {
                            val dec = decryptJioSaavnUrl(encMedia)
                            if (dec.startsWith("http")) {
                                sStream = dec.replace("_96.mp4", "_320.mp4").replace("_96.m4a", "_320.mp4")
                            }
                        }
                        songList.add(
                            Song(
                                id = sId,
                                title = sTitle,
                                artist = if (sArtist.isNotBlank()) sArtist else artist,
                                album = title,
                                durationSec = sDur,
                                artworkUrl = sImg,
                                stream160Url = sStream,
                                stream320Url = sStream
                            )
                        )
                    }
                }
                return Album(
                    id = id,
                    title = title,
                    artist = if (artist.isNotBlank()) artist else "Featured Release",
                    artworkUrl = img,
                    trackCount = songList.size,
                    songs = songList
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun fetchJioSaavnPlaylistDetails(id: String): Playlist? {
        try {
            val url = URL("https://www.jiosaavn.com/api.php?__call=playlist.getDetails&listid=$id&_format=json&_marker=0&ctx=web6dot0")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                val title = cleanSaavnText(json.optString("title"))
                val img = json.optString("image").replace("150x150", "500x500")
                val songsArr = json.optJSONArray("songs")
                val songList = mutableListOf<Song>()
                if (songsArr != null) {
                    for (i in 0 until songsArr.length()) {
                        val s = songsArr.getJSONObject(i)
                        val sId = s.optString("id")
                        val sTitle = cleanSaavnText(s.optString("song"))
                        val sArtist = cleanSaavnText(s.optString("primary_artists"))
                        val sAlbum = cleanSaavnText(s.optString("album"))
                        val sDur = s.optInt("duration", 0)
                        val sImg = s.optString("image").replace("150x150", "500x500")
                        val encMedia = s.optString("encrypted_media_url")
                        var sStream = ""
                        if (encMedia.isNotBlank()) {
                            val dec = decryptJioSaavnUrl(encMedia)
                            if (dec.startsWith("http")) {
                                sStream = dec.replace("_96.mp4", "_320.mp4").replace("_96.m4a", "_320.mp4")
                            }
                        }
                        songList.add(
                            Song(
                                id = sId,
                                title = sTitle,
                                artist = if (sArtist.isNotBlank()) sArtist else "Artist",
                                album = sAlbum,
                                durationSec = sDur,
                                artworkUrl = sImg,
                                stream160Url = sStream,
                                stream320Url = sStream
                            )
                        )
                    }
                }
                return Playlist(
                    id = id,
                    title = title,
                    artworkUrl = img,
                    trackCount = songList.size,
                    songs = songList
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }



    private fun fetchJioSaavnRadioTracks(songId: String): List<Song> {
        try {
            val url = URL("https://www.jiosaavn.com/api.php?__call=reco.getreco&pid=$songId&_format=json&_marker=0&ctx=android")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val list = mutableListOf<Song>()
                if (text.trim().startsWith("{")) {
                    val json = JSONObject(text)
                    val arr = json.optJSONArray(songId)
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val s = arr.getJSONObject(i)
                            val id = s.optString("id")
                            val title = cleanSaavnText(s.optString("song"))
                            val artist = cleanSaavnText(s.optString("primary_artists"))
                            val album = cleanSaavnText(s.optString("album"))
                            val dur = s.optInt("duration", 0)
                            val img = s.optString("image").replace("150x150", "500x500")
                            list.add(Song(id = id, title = title, artist = artist, album = album, durationSec = dur, artworkUrl = img))
                        }
                    }
                } else if (text.trim().startsWith("[")) {
                    val arr = org.json.JSONArray(text)
                    for (i in 0 until arr.length()) {
                        val s = arr.getJSONObject(i)
                        val id = s.optString("id")
                        val title = cleanSaavnText(s.optString("song"))
                        val artist = cleanSaavnText(s.optString("primary_artists"))
                        val album = cleanSaavnText(s.optString("album"))
                        val dur = s.optInt("duration", 0)
                        val img = s.optString("image").replace("150x150", "500x500")
                        list.add(Song(id = id, title = title, artist = artist, album = album, durationSec = dur, artworkUrl = img))
                    }
                }
                return list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    private fun fetchLrclibLyrics(title: String, artist: String): String {
        try {
            val encTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val encArtist = java.net.URLEncoder.encode(artist, "UTF-8")
            val url = URL("https://lrclib.net/api/get?track_name=$encTitle&artist_name=$encArtist")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "PulseMusic/1.0")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                val plain = json.optString("plainLyrics", "")
                if (plain.isNotBlank()) return plain
                val synced = json.optString("syncedLyrics", "")
                if (synced.isNotBlank()) {
                    return synced.lines().map { line ->
                        line.replace(Regex("^\\[\\d+:\\d+(\\.\\d+)?\\]\\s*"), "")
                    }.filter { it.isNotBlank() }.joinToString("\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun fetchJioSaavnLyrics(lyricsId: String): String {
        try {
            val url = URL("https://www.jiosaavn.com/api.php?__call=lyrics.getLyrics&lyrics_id=$lyricsId&_format=json&_marker=0&ctx=web6dot0")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                val lyrics = json.optString("lyrics", "")
                if (lyrics.isNotBlank()) {
                    return cleanSaavnText(lyrics.replace("<br>", "\n").replace("<br/>", "\n"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun cleanSaavnText(text: String): String {
        return text.replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    
    private fun decryptJioSaavnUrl(url: String): String {
        try {
            val key = "38346591"
            val secretKey = javax.crypto.spec.SecretKeySpec(key.toByteArray(Charsets.UTF_8), "DES")
            val cipher = javax.crypto.Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey)
            
            val decodedBytes = android.util.Base64.decode(url.trim(), android.util.Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    private fun fetchSoundCloudStreamUrl(title: String, artist: String): String {
        return SoundCloudClient.resolveStreamUrl("", title, artist)
    }

    private fun fetchJioSaavnStreamUrl(title: String, artist: String): String {
        try {
            if (title.isBlank()) return ""
            val searchQueries = listOf(
                "$title $artist".trim(),
                title.trim()
            ).distinct()

            for (cleanQuery in searchQueries) {
                val encodedQuery = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
                val searchUrl = java.net.URL("https://www.jiosaavn.com/api.php?__call=search.getResults&q=$encodedQuery&_format=json&_marker=0&api_version=4&ctx=android&n=5&p=1")
                val searchConn = searchUrl.openConnection() as java.net.HttpURLConnection
                searchConn.requestMethod = "GET"
                searchConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                searchConn.setRequestProperty("Accept", "application/json")
                searchConn.connectTimeout = 4000
                searchConn.readTimeout = 4000

                if (searchConn.responseCode == 200) {
                    val searchRes = searchConn.inputStream.bufferedReader().readText()
                    val jsonObj = org.json.JSONObject(searchRes)
                    val results = jsonObj.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        for (i in 0 until results.length()) {
                            val candidate = results.getJSONObject(i)
                            val jioId = candidate.optString("id")
                            if (jioId.isNotBlank()) {
                                val detailsUrl = java.net.URL("https://www.jiosaavn.com/api.php?__call=song.getDetails&pids=$jioId&_format=json&_marker=0&ctx=android")
                                val dConn = detailsUrl.openConnection() as java.net.HttpURLConnection
                                dConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                dConn.connectTimeout = 4000
                                dConn.readTimeout = 4000
                                if (dConn.responseCode == 200) {
                                    val dRes = dConn.inputStream.bufferedReader().readText()
                                    val dJson = org.json.JSONObject(dRes).optJSONObject(jioId)
                                    if (dJson != null) {
                                        val encMediaUrl = dJson.optString("encrypted_media_url")
                                        if (encMediaUrl.isNotBlank()) {
                                            val dec = decryptJioSaavnUrl(encMediaUrl)
                                            if (dec.startsWith("http")) {
                                                return dec.replace("_96.mp4", "_320.mp4").replace("_96.m4a", "_320.mp4")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun fetchYouTubeStreamUrl(id: String): String {
        try {
            if (id.isBlank()) return ""
            val url = "https://www.youtube.com/watch?v=$id"
            val extractor = org.schabi.newpipe.extractor.NewPipe.getService(0).getStreamExtractor(url)
            extractor.fetchPage()
            val audioStreams = extractor.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                val bestStream = audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams.first()
                val contentUrl = bestStream.content
                if (!contentUrl.isNullOrBlank()) return contentUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private suspend fun fetchStreamUrl(id: String, title: String = "", artist: String = ""): String {
        return withContext(Dispatchers.IO) {
            val provider = _userSettingsFlow.value.provider
            
            // NewPipe (YouTube) is the new primary engine.
            val yt = fetchYouTubeStreamUrl(id)
            if (yt.isNotBlank()) return@withContext yt
            
            when (provider) {
                MusicProvider.JIOSAAVN -> {
                    val jio = fetchJioSaavnStreamUrl(title, artist)
                    if (jio.isNotBlank()) return@withContext jio
                    val sc = fetchSoundCloudStreamUrl(title, artist)
                    if (sc.isNotBlank()) return@withContext sc
                }
                MusicProvider.SOUNDCLOUD -> {
                    val sc = fetchSoundCloudStreamUrl(title, artist)
                    if (sc.isNotBlank()) return@withContext sc
                    val jio = fetchJioSaavnStreamUrl(title, artist)
                    if (jio.isNotBlank()) return@withContext jio
                }
                MusicProvider.YOUTUBE -> {
                    val jio = fetchJioSaavnStreamUrl(title, artist)
                    if (jio.isNotBlank()) return@withContext jio
                    val sc = fetchSoundCloudStreamUrl(title, artist)
                    if (sc.isNotBlank()) return@withContext sc
                }
                MusicProvider.AUTO -> {
                    val jio = fetchJioSaavnStreamUrl(title, artist)
                    if (jio.isNotBlank()) return@withContext jio
                    val sc = fetchSoundCloudStreamUrl(title, artist)
                    if (sc.isNotBlank()) return@withContext sc
                    if (yt.isNotBlank()) return@withContext yt
                }
            }
            ""
        }
    }

    override fun getTrendingSongs(): Flow<List<Song>> = getTrendingSongsFlow(api).flowOn(Dispatchers.IO)
    override fun getFeaturedAlbums(): Flow<List<Album>> = getFeaturedAlbumsFlow(api).flowOn(Dispatchers.IO)
    override fun getFeaturedPlaylists(): Flow<List<Playlist>> = getFeaturedPlaylistsFlow(api).flowOn(Dispatchers.IO)

    override fun getUserPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { it.map { entity -> Playlist(entity.id, entity.title, entity.description, entity.artworkUrl, 0, entity.creator, entity.createdAt) } }
    
    override fun getPlaylistSongs(playlistId: String): Flow<List<Song>> = playlistDao.getSongsForPlaylist(playlistId).map { it.map { entity -> Song(entity.id, entity.title, entity.artist, entity.album, entity.durationSec, entity.artworkUrl, entity.stream160Url, entity.stream320Url, entity.lyrics, entity.isDownloaded, entity.isFavorite, entity.localFilePath, entity.year, entity.artistId, entity.albumId) } }
    
    override suspend fun createPlaylist(title: String, description: String): String {
        val id = java.util.UUID.randomUUID().toString()
        playlistDao.insertPlaylist(com.example.data.local.entity.PlaylistEntity(id, title, description, "", "Pulse Music", System.currentTimeMillis()))
        return id
    }
    
    override suspend fun deletePlaylist(playlistId: String) = playlistDao.deletePlaylist(playlistId)
    override suspend fun addSongToPlaylist(playlistId: String, song: Song) {
        playlistDao.insertPlaylistSong(com.example.data.local.entity.PlaylistSongCrossRef(playlistId, song.id, System.currentTimeMillis().toInt()))
        songDao.insertSong(com.example.data.local.entity.SongEntity(song.id, song.title, song.artist, song.album, song.durationSec, song.artworkUrl, song.stream160Url, song.stream320Url, song.lyrics, song.isDownloaded, song.isFavorite, song.localFilePath, song.year, song.artistId, song.albumId, System.currentTimeMillis()))
    }
    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String) = playlistDao.removeSongFromPlaylist(playlistId, songId)
    
    override fun getFavoriteSongs(): Flow<List<Song>> = songDao.getFavoriteSongs().map { it.map { entity -> Song(entity.id, entity.title, entity.artist, entity.album, entity.durationSec, entity.artworkUrl, entity.stream160Url, entity.stream320Url, entity.lyrics, entity.isDownloaded, entity.isFavorite, entity.localFilePath, entity.year, entity.artistId, entity.albumId) } }
    override suspend fun toggleFavorite(song: Song) {
        val current = songDao.getSongById(song.id)
        if (current != null) {
            songDao.updateFavorite(song.id, !current.isFavorite)
        } else {
            songDao.insertSong(com.example.data.local.entity.SongEntity(song.id, song.title, song.artist, song.album, song.durationSec, song.artworkUrl, song.stream160Url, song.stream320Url, song.lyrics, song.isDownloaded, true, song.localFilePath, song.year, song.artistId, song.albumId, System.currentTimeMillis()))
        }
    }
    
    override fun getDownloadedSongs(): Flow<List<Song>> = songDao.getDownloadedSongs().map { it.map { entity -> Song(entity.id, entity.title, entity.artist, entity.album, entity.durationSec, entity.artworkUrl, entity.stream160Url, entity.stream320Url, entity.lyrics, entity.isDownloaded, entity.isFavorite, entity.localFilePath, entity.year, entity.artistId, entity.albumId) } }
    
    override suspend fun downloadSong(song: Song): Result<String> = withContext(Dispatchers.IO) {
        var targetSong = song
        if (targetSong.getStreamUrl(preferHighQuality = true).isBlank()) {
            val resolved = resolveStreamUrl(targetSong)
            if (resolved.isNotBlank()) {
                targetSong = targetSong.copy(stream160Url = resolved, stream320Url = resolved)
            }
        }
        val res = downloadManager.downloadSong(targetSong)
        if (res.isSuccess) {
            val path = res.getOrNull()
            val updated = targetSong.copy(isDownloaded = true, localFilePath = path)
            songCache[song.id] = updated
        }
        res
    }
    
    override suspend fun removeDownload(songId: String): Unit = withContext(Dispatchers.IO) {
        downloadManager.removeDownload(songId)
        songCache[songId]?.let {
            songCache[songId] = it.copy(isDownloaded = false, localFilePath = null)
        }
        Unit
    }

    override suspend fun getLocalDownloadPath(songId: String): String? = withContext(Dispatchers.IO) {
        val cached = songDao.getSongById(songId)
        if (cached != null && cached.isDownloaded && !cached.localFilePath.isNullOrBlank()) {
            val file = java.io.File(cached.localFilePath)
            if (file.exists() && file.length() > 0L) {
                return@withContext cached.localFilePath
            }
        }
        val file = downloadManager.getDownloadedFile(songId)
        if (file != null && file.exists() && file.length() > 0L) {
            songDao.updateDownloaded(songId, true, file.absolutePath)
            return@withContext file.absolutePath
        }
        null
    }

    override fun getUserSettings(): Flow<UserSettings> = _userSettingsFlow.asStateFlow()

    override suspend fun updateSettings(settings: UserSettings) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString("audio_quality", settings.audioQuality.name)
            .putInt("cache_size_limit_mb", settings.cacheSizeLimitMb)
            .putInt("crossfade_sec", settings.crossfadeDurationSec)
            .putBoolean("is_dark_theme", settings.isDarkTheme)
            .putBoolean("auto_skip_failed", settings.autoSkipFailedTracks)
            .putBoolean("autoplay_enabled", settings.autoplayEnabled)
            .putString("custom_api_url", settings.customApiBaseUrl)
            .putString("api_provider", settings.apiProvider)
            .apply()
        _userSettingsFlow.value = settings
    }

    override suspend fun setProvider(provider: MusicProvider) = withContext(Dispatchers.IO) {
        prefs.edit().putString("api_provider", provider.id).apply()
        _userSettingsFlow.value = _userSettingsFlow.value.copy(apiProvider = provider.id)
    }

    override suspend fun checkAllProviders(): Map<MusicProvider, ProviderStatus> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<MusicProvider, ProviderStatus>()

        // 1. Check JioSaavn
        try {
            val start = System.currentTimeMillis()
            val url = java.net.URL("https://www.jiosaavn.com/api.php?__call=search.getResults&q=starboy&_format=json&n=1")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val code = conn.responseCode
            val latency = System.currentTimeMillis() - start
            if (code == 200) {
                results[MusicProvider.JIOSAAVN] = ProviderStatus(
                    provider = MusicProvider.JIOSAAVN,
                    isOnline = true,
                    latencyMs = latency,
                    statusMessage = "320kbps Streams • Online"
                )
            } else {
                results[MusicProvider.JIOSAAVN] = ProviderStatus(
                    provider = MusicProvider.JIOSAAVN,
                    isOnline = false,
                    latencyMs = latency,
                    statusMessage = "HTTP $code"
                )
            }
        } catch (e: Exception) {
            results[MusicProvider.JIOSAAVN] = ProviderStatus(
                provider = MusicProvider.JIOSAAVN,
                isOnline = false,
                latencyMs = 0L,
                statusMessage = e.localizedMessage ?: "Connection error"
            )
        }

        // 2. Check SoundCloud
        try {
            val (isOnline, latency) = SoundCloudClient.checkHealth()
            if (isOnline) {
                results[MusicProvider.SOUNDCLOUD] = ProviderStatus(
                    provider = MusicProvider.SOUNDCLOUD,
                    isOnline = true,
                    latencyMs = latency,
                    statusMessage = "HQ Progressive/HLS • Online"
                )
            } else {
                results[MusicProvider.SOUNDCLOUD] = ProviderStatus(
                    provider = MusicProvider.SOUNDCLOUD,
                    isOnline = false,
                    latencyMs = latency,
                    statusMessage = "Offline / Connection error"
                )
            }
        } catch (e: Exception) {
            results[MusicProvider.SOUNDCLOUD] = ProviderStatus(
                provider = MusicProvider.SOUNDCLOUD,
                isOnline = false,
                latencyMs = 0L,
                statusMessage = e.localizedMessage ?: "Connection error"
            )
        }

        // 3. Check YouTube Music (using simple HEAD since NewPipe handles extraction)
        try {
            val start = System.currentTimeMillis()
            val url = java.net.URL("https://music.youtube.com")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "HEAD"
            val code = conn.responseCode
            val latency = System.currentTimeMillis() - start
            if (code == 200) {
                results[MusicProvider.YOUTUBE] = ProviderStatus(
                    provider = MusicProvider.YOUTUBE,
                    isOnline = true,
                    latencyMs = latency,
                    statusMessage = "Stable (NewPipe Engine) • Online"
                )
            } else {
                results[MusicProvider.YOUTUBE] = ProviderStatus(
                    provider = MusicProvider.YOUTUBE,
                    isOnline = false,
                    latencyMs = latency,
                    statusMessage = "HTTP $code"
                )
            }
        } catch (e: Exception) {
            results[MusicProvider.YOUTUBE] = ProviderStatus(
                provider = MusicProvider.YOUTUBE,
                isOnline = false,
                latencyMs = 0L,
                statusMessage = e.localizedMessage ?: "Connection error"
            )
        }

        // 4. Auto is operational if at least one provider is operational
        val operationalCount = results.count { it.value.isOnline }
        results[MusicProvider.AUTO] = ProviderStatus(
            provider = MusicProvider.AUTO,
            isOnline = operationalCount > 0,
            latencyMs = results.values.filter { it.isOnline }.map { it.latencyMs }.minOrNull() ?: 0L,
            statusMessage = "Smart Routing ($operationalCount/3 providers online)"
        )

        results
    }

    override fun getCacheSizeBytes(): Long = mediaCacheManager.getUsedCacheSizeBytes()
    override suspend fun clearCache() = mediaCacheManager.clearMediaCache()
    override suspend fun testApiEndpoint(url: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            val u = java.net.URL(if (url.startsWith("http")) url else "https://$url")
            val conn = u.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "PulseMusic/1.0")
            val code = conn.responseCode
            val latency = System.currentTimeMillis() - start
            if (code in 200..399) {
                Result.success(latency)
            } else {
                Result.failure(Exception("HTTP Error $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun importSpotifyPlaylist(url: String): Flow<ImportProgress> = kotlinx.coroutines.flow.flow {
        try {
            emit(ImportProgress(0.0f, "Extracting Spotify Playlist ID..."))
            val idMatch = Regex("open\\.spotify\\.com/playlist/([a-zA-Z0-9]+)").find(url)
            val playlistId = idMatch?.groupValues?.get(1) ?: run {
                emit(ImportProgress(1f, "Invalid Spotify URL", isComplete = true, error = "Could not parse playlist ID."))
                return@flow
            }

            emit(ImportProgress(0.1f, "Fetching playlist metadata..."))
            val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
            
            val connection = withContext(Dispatchers.IO) {
                val urlObj = URL(embedUrl)
                urlObj.openConnection() as HttpURLConnection
            }.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                emit(ImportProgress(1f, "Failed to connect to Spotify", isComplete = true, error = "HTTP ${connection.responseCode}"))
                return@flow
            }

            val html = withContext(Dispatchers.IO) {
                connection.inputStream.bufferedReader().use { it.readText() }
            }

            val jsonMatch = Regex("<script id=\"__NEXT_DATA__\" type=\"application/json\">(.*?)</script>").find(html)\n            if (jsonMatch == null) {
                emit(ImportProgress(1f, "Could not find playlist data", isComplete = true, error = "Failed to parse Spotify embed page."))
                return@flow
            }

            val jsonStr = jsonMatch.groupValues[1]
            val json = JSONObject(jsonStr)
            
            val entity = json.optJSONObject("props")?.optJSONObject("pageProps")?.optJSONObject("state")?.optJSONObject("data")?.optJSONObject("entity")
            if (entity == null) {
                emit(ImportProgress(1f, "Invalid playlist data format", isComplete = true, error = "Missing entity object."))
                return@flow
            }

            val title = entity.optString("name", "Imported Spotify Playlist")
            val trackList = entity.optJSONArray("trackList") ?: JSONArray()
            
            if (trackList.length() == 0) {
                emit(ImportProgress(1f, "Playlist is empty", isComplete = true, error = "No tracks found."))
                return@flow
            }

            val totalTracks = trackList.length()
            emit(ImportProgress(0.2f, "Found $totalTracks tracks. Creating local playlist..."))

            // Create local playlist
            val dbPlaylistId = createPlaylist(title, "Imported from Spotify")
            var matchedTracks = 0

            for (i in 0 until trackList.length()) {
                val trackObj = trackList.optJSONObject(i) ?: continue
                val trackTitle = trackObj.optString("title", "")
                val trackArtist = trackObj.optString("subtitle", "")
                
                if (trackTitle.isBlank()) continue

                val query = "$trackArtist - $trackTitle"
                emit(ImportProgress(0.2f + (0.7f * (i.toFloat() / totalTracks.toFloat())), "Matching: $trackTitle"))

                try {
                    val searchResults = searchAll(query)
                    val bestMatch = searchResults.songs.firstOrNull()
                    
                    if (bestMatch != null) {
                        addSongToPlaylist(dbPlaylistId, bestMatch)
                        matchedTracks++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            emit(ImportProgress(1f, "Successfully imported $matchedTracks/$totalTracks tracks!", isComplete = true, playlistId = dbPlaylistId))

        } catch (e: Exception) {
            e.printStackTrace()
            emit(ImportProgress(1f, "Import failed", isComplete = true, error = e.message))
        }
    }.flowOn(Dispatchers.IO)
}