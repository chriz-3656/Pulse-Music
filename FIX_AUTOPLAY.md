# Fixing Autoplay & Content-Based Recommendation in Pulse Music

## The Issue
Currently, when a user searches for a song and taps on a result, the app queues the **entire list of search results** rather than generating a smart, content-based radio station based on the selected track. 

Because the player queue is immediately filled with 20+ unrelated search results, the `MusicPlayerController` never reaches the end of the queue to trigger the `autoplayProvider` (which fetches related songs). Instead, it just blindly plays the next item in the search results.

## Root Cause Analysis
1. **The Backend is already capable:** Your `MusicRepositoryImpl.kt` already has the `getSongSuggestions(songId)` function implemented using YouTube Music's `SongRadio` endpoint and JioSaavn fallback.
2. **The Player Controller is already capable:** In `MusicPlayerController.kt`, there is explicit logic that checks `if (queue.size == 1 && isAutoplayEnabled)` and proactively pre-fetches `autoplayProvider?.invoke(song.id)` to build an infinite stream of similar music.
3. **The UI is the bottleneck:** In `SearchScreen.kt`, when a user clicks a song, the callback is:
   ```kotlin
   onClick = { viewModel.playSong(song, results.songs) }
   ```
   This passes the *entire search result list* to the player.

## How to Fix It

To achieve true content-based recommendation (autoplay), you need to change the default behavior of selecting a search result so that it acts as a "seed" for a new radio station, rather than queuing the whole search list.

### Step 1: Update `MusicViewModels.kt`
Add a specific method to handle "Radio/Autoplay" playback which intentionally passes only the selected song to the player.

```kotlin
// In your view models (e.g., SearchViewModel)
fun playSongAsRadio(song: Song) {
    // By passing only a single song, we force the MusicPlayerController 
    // to trigger the autoplayProvider and fetch similar, content-based recommendations.
    playerController.playSong(song, listOf(song))
}
```

### Step 2: Update `SearchScreen.kt`
Modify the `onClick` handlers for search results to use this new method.

**Before:**
```kotlin
onClick = { viewModel.playSong(song, results.songs) }
```

**After:**
```kotlin
onClick = { viewModel.playSongAsRadio(song) }
```
*(You should do this for `results.songs`, `results.similarTracks`, and other search lists where the user expects a radio-like experience).*

### Step 3: Verify `MusicPlayerController.kt` Autoplay Logic
Ensure that your `autoplayProvider` logic inside `MusicPlayerController.kt` correctly appends the fetched suggestions to the queue. 

```kotlin
// Inside playSong()
if (queue.size == 1 && isAutoplayEnabled && autoplayProvider != null) {
    scope.launch {
        try {
            val suggestions = autoplayProvider?.invoke(song.id) ?: emptyList()
            if (suggestions.isNotEmpty()) {
                val current = _playerState.value
                if (current.currentSong?.id == song.id) {
                    // Filter out the current song to avoid duplicates
                    val filtered = suggestions.filter { it.id != song.id }
                    // Append true content-based recommendations to the queue
                    _playerState.update { it.copy(queue = current.queue + filtered) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
```

## Why this matters (Content-Based Autoplay)
By making this fix, Pulse Music will leverage **content-based recommendation**. Instead of playing songs that happen to share a keyword in a search query, the underlying `SongRadio` API analyzes acoustic properties (tempo, genre, mood) and metadata embeddings to queue tracks that are sonically and thematically similar to the seed track. This is what provides users with an endless, high-quality "Flow" or "Autoplay" experience similar to premium streaming apps.
