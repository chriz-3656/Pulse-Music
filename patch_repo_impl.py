import re

with open("app/src/main/java/com/example/data/repository/MusicRepositoryImpl.kt", "r") as f:
    text = f.read()

import_method = """
    override fun importSpotifyPlaylist(url: String): Flow<ImportProgress> = kotlinx.coroutines.flow.flow {
        try {
            emit(ImportProgress(0.0f, "Extracting Spotify Playlist ID..."))
            val idMatch = Regex("open\\\\.spotify\\\\.com/playlist/([a-zA-Z0-9]+)").find(url)
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

            val jsonMatch = Regex("<script id=\\"__NEXT_DATA__\\" type=\\"application/json\\">(.*?)</script>").find(html)
            if (jsonMatch == null) {
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
}"""

# Insert right before the last closing brace of MusicRepositoryImpl
text = re.sub(r'}\s*$', import_method, text)

with open("app/src/main/java/com/example/data/repository/MusicRepositoryImpl.kt", "w") as f:
    f.write(text)
