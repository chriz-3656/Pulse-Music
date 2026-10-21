---
name: media3-exoplayer-streaming
description: >-
  Specialized engineering patterns for AndroidX Media3 ExoPlayer, MediaSessionService,
  background audio playback, WakeLock/WifiLock continuous playback, audio caching, and
  multi-source stream resolution (JioSaavn, SoundCloud, YouTube Music). Use when working
  on playback engine issues, streaming fallbacks, background service lifecycles, or real-time
  audio synchronization.
metadata:
  model: inherit
---

# Media3 ExoPlayer & Audio Streaming Patterns

This skill provides domain-specific guidelines, lifecycle requirements, and error-handling patterns for AndroidX Media3 ExoPlayer in Pulse Music.

---

## 1. Core Architecture & Components

Pulse Music manages audio playback through a decoupled architecture:
* **`MusicPlaybackService`**: Foreground Android Service managing the `ExoPlayer` instance, Android system `MediaSession`, status bar playback notifications, and CPU `WakeLock`s.
* **`MusicPlayerController`**: High-level application controller orchestrating UI StateFlows (`PlayerState`), queue management, shuffle/repeat modes, and user playback requests.
* **`MediaCacheManager` & `DynamicCacheEvictor`**: Manages ExoPlayer `SimpleCache` disk quotas and LRU eviction for downloaded/cached streams.

---

## 2. ExoPlayer State & Autoplay Rules

### A. `isPlaying` vs `playWhenReady`
* **Never** rely solely on `exoPlayer.isPlaying` to represent user intent in background services.
* `isPlaying` drops to `false` during buffering, track transitions, and stream resolution.
* Use `exoPlayer.playWhenReady` combined with `playbackState` to determine actual playback status:
  ```kotlin
  val isBuffering = playbackState == Player.STATE_BUFFERING
  val playWhenReady = exoPlayer.playWhenReady
  playerController.updatePlaybackState(playWhenReady, isBuffering)
  ```

### B. Track Transitions & Autoplay
* When advancing through queues (Spotify imports, radio queues, or albums), ensure `exoPlayer.setMediaItem()` is followed by:
  ```kotlin
  exoPlayer.prepare()
  if (currentState.isPlaying || exoPlayer.playWhenReady) {
      exoPlayer.play()
  }
  ```

---

## 3. Screen-Off Playback & WakeLocks

To prevent Android Doze mode and power management from halting background playback:
1. **CPU WakeLock**: Acquire a partial wake lock with a conservative timeout (e.g., 30s) while resolving asynchronous stream URLs:
   ```kotlin
   fetchWakeLock = powerManager.newWakeLock(
       PowerManager.PARTIAL_WAKE_LOCK,
       "PulseMusic:FetchWakeLock"
   )
   ```
   * Always wrap async fetch operations in `try ... finally` blocks and release `fetchWakeLock` once ExoPlayer begins buffering.
2. **ExoPlayer Network Wake Mode**:
   ```kotlin
   ExoPlayer.Builder(context)
       .setWakeMode(C.WAKE_MODE_NETWORK)
       .build()
   ```
3. **Audio Becoming Noisy**:
   * Enable `.setHandleAudioBecomingNoisy(true)` to automatically pause playback when headphones are disconnected.

---

## 4. Multi-Source Streaming & Fallback Routing

When preparing a track for playback, route through providers with fallback redundancy:
1. **Local Download First**: Check `musicRepository.getLocalDownloadPath(song.id)` or `song.localFilePath`. If valid on disk, play local file URI.
2. **JioSaavn Engine**: Primary source for high-bitrate Indian regional & popular music (up to 320kbps).
3. **SoundCloud Engine**: High-fidelity alternative for indie, electronic, and global audio catalogs.
4. **YouTube Music (NewPipe / ytm-kt)**: Universal fallback for global music and long-tail catalog coverage.

---

## 5. JAM Session & Live Sync Room Rules

When synchronizing playback across devices in Pulse JAM:
* **Wait for Buffer Readiness**: Do not apply timestamp seeks while `Player.STATE_BUFFERING` is active. Applying position changes before buffering completes causes endless seek loops and audio stutter.
* **Drift Threshold**: Only apply hard seek corrections if playback drift relative to the host exceeds 3,000ms. Minor sub-second drift should be ignored to avoid audio glitches.
