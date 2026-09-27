# Fixing the Global 0:00 Duration Bug in Pulse Music

## The Issue
Across Pulse Music, almost all track durations displayed as `0:00`. This issue plagued the Search Screen, the Home Feed, the Mini Player, the Full-Screen Player, the Android MediaSession Notification, and even tracks imported from Spotify Playlists. 

Because the `0:00` glitch appeared uniformly across entirely different UI surfaces, it pointed to a deeply rooted data propagation problem rather than an isolated UI bug.

## Root Cause Analysis
We discovered a compound failure where multiple independent bugs conspired to zero out the duration of tracks:

1. **The `ytm-kt` Parsing Limitation:**
   When searching or scraping playlists from YouTube Music (the default fallback provider), the internal JSON parser in `ytm-kt` was failing to extract track lengths from the raw API response. Because the library returned `null` for `duration`, Pulse Music's domain mapper (`YtmSong.toDomain`) forcibly defaulted to `0L`. This immediately broke track lists (Search, Home, Playlists).

2. **Mixed Conventions (Seconds vs Milliseconds):**
   The Pulse domain models (`Song`, `PlayerState`) strictly expected durations to be in **milliseconds**. However, mappers for alternative providers (JioSaavn, SoundCloud) were originally passing values in **seconds**. A 3-minute track (180s) was mapped as `180L` instead of `180000L`. The UI formatter subsequently divided `180 / 1000 = 0`, yielding `0:00`.

3. **Asynchronous Late Duration Discovery (The HLS/Extractor Problem):**
   Even after fixing the provider mappers, the active playback state remained stuck at `0:00`. We discovered that `MusicPlaybackService` was attempting to read `exoPlayer.duration` at the precise moment the player fired `Player.STATE_READY`. 
   
   However, for stream extractors (like `NewPipeExtractor`) and HLS streams, ExoPlayer does **not** know the length of the track immediately upon readiness. At `STATE_READY`, it returns a negative placeholder (`C.TIME_UNSET`). Because the service checked it exactly once and never again, it missed the true duration when ExoPlayer eventually discovered it milliseconds later via its internal timeline updater.

## How to Fix It (Applied Changes)

### Step 1: Standardize Milliseconds
All domain models, DAOs, and network mappers were heavily audited and forced into a strict `Long` millisecond convention. 
- JioSaavn parses seconds: `duration = sDur * 1000L`
- SoundCloud parses milliseconds: `duration = durationMs`

### Step 2: Proactive Polling for Late Discoveries
We refactored `MusicPlaybackService.kt` to continuously watch for the exact moment ExoPlayer resolves the true track length.

```kotlin
// Inside startProgressPolling()
val dur = exoPlayer.duration
playerController.updateProgress(cur, dur, buf)

// If ExoPlayer has discovered a valid, positive duration, sync it!
if (dur > 0) {
    playerController.playerState.value.currentSong?.let { song ->
        if (song.duration <= 0L || song.duration != dur) {
            // Instantly updates the Mini-Player, Full-Screen Player, and MediaSession
            playerController.updateCurrentSongDuration(dur)
            
            // Asynchronously heal the Room database so it loads correctly next time
            serviceScope.launch(Dispatchers.IO) {
                musicRepository.updateSongDuration(song.id, dur)
            }
        }
    }
}
```

### Step 3: Graceful UI Fallbacks (`--:--`)
To fix the visual glitch caused by the `ytm-kt` parsing limitation (where tracks initially load with `0` duration before playback), we implemented a UX mask in the UI formatters.

```kotlin
val formattedDuration: String
    get() {
        if (duration <= 0L) return "--:--"
        val minutes = (duration / 1000) / 60
        val seconds = (duration / 1000) % 60
        return String.format("%d:%02d", minutes, seconds)
    }
```
Now, unknown tracks elegantly display `--:--` in search results. As soon as a user clicks play, ExoPlayer organically discovers the true duration, triggers the polling observer, and dynamically morphs the `--:--` into the actual timecode (e.g., `03:42`) across the entire app.

## Why this matters
By patching the duration pipeline from the network layer all the way to the Android MediaSession, we restored crucial user-facing features (progress scrubbing, track lengths, lock-screen seek bars) and eliminated a jarring visual bug that broke the premium feel of the application.
