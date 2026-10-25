# Changelog

## [2.7.1] - 2026-10-23
### Added
- **Dynamic Blurred Lock Screen Album Art:** Native lock screen wallpaper dynamically mirrors the currently playing track's artwork with a slightly zoomed (1.12x), fast StackBlur (radius 22), and 28% dark scrim for system clock and notification legibility. Includes an on/off toggle under Settings ("Lock Screen & Visuals") defaulting to off, with automatic cleanup on playback stop/service destroy.

### Security
- **In-App Update Verification:** The updater now requires HTTPS, verifies the downloaded APK's package name, version code, and SHA-256 signing-certificate digest against the installed app, checks DownloadManager's final status, and fails closed on any mismatch. Update files are copied into a private cache directory and exposed through a minimal FileProvider path.
- **Pulse JAM Authorization:** Room creation, joining, kick, end-room, queue mutation, playback updates, and skip votes are checked against the authenticated Firebase user. Host-only actions are enforced in the client and documented in `database.rules.json`; room creation, participant limits, and concurrent queue updates are handled safely.
- **Network Hardening:** Android cleartext traffic is disabled, custom API endpoints must use HTTPS, API/media clients reject requests in Pure Offline Mode, and HTTP logging is debug-only.
- **Release Signing:** Release builds fail if `release.keystore` or its credentials are missing instead of silently falling back to the debug key. CI verifies the release APK signature against the configured keystore.

### Changed
- **Release Stability & Full Compatibility:** Release builds preserve the complete, unminified bytecode across all 5 DEX files, ensuring 100% stability with zero reflection or constructor stripping.
- **Crash Resilience & Startup Safety:** Added Room database migration fallback (`fallbackToDestructiveMigration`) to prevent schema upgrade crashes, lazy initialization for Firebase Realtime Database & Auth (`JamSessionManager`), lazy initialization for YouTube Music (`MusicRepositoryImpl`), and main-thread exception guards on application startup settings sync.
- **Privacy:** Removed the unused Firebase Analytics dependency. Android cloud backup is disabled. Third-party notices and the GPL-3.0 license are included in the repository.
- **Offline Behavior:** Pure Offline Mode now guards search suggestions, song details, stream resolution, batch song loads, lyrics, suggestions, album/playlist/artist details, JAM sessions, the kill-switch check, and the update check. Cached artist pages fall back to Room data.
- **Build and CI:** Added `fix/**` branch triggers, unit tests for version comparison and custom API URL merging, APK signature verification, test-report and R8 mapping artifacts, and required Firebase/keystore secrets.
- **Repository Cleanup:** Removed the unused vendored `ytm-kt/` source tree; the app uses the published `dev.toastbits.ytmkt:ytmkt:0.3.2` artifact.

### Fixed
- **Custom API URL Paths:** The custom endpoint interceptor now preserves the configured base path, avoids duplicated boundary segments, and rejects invalid or non-HTTPS URLs.
- **Cache Limit Setting:** The configured cache size limit is now applied to the media cache at startup.
- **Artist Details:** Artist profiles, top songs, and albums now use cached Room data when available and fall back between remote layouts instead of returning empty lists.

## [2.7.0] - 2026-10-21
### Added
- **Instant Live Download Queue & Offline Section Population:** Starting a download for a single track, an album, or an entire playlist immediately registers all songs in the offline downloads archive. Users immediately see all queued and downloading tracks with active state indications.
- **Live Byte-Streaming Download Progress:** Download tasks report real-time byte progress with animated progress bars and percentage displays (`DOWNLOADING 45%`), while upcoming songs display `QUEUED IN LINE`. Completed tracks automatically transition into the `OFFLINE MASTER COPIES` library.
- **Batch Album & Playlist Downloads:** One-tap full download button on Album and Playlist detail screens with live progress counter (`DOWNLOAD` → `DOWNLOADING X/Y` → `OFFLINE`) and instant batch removal.
- **Dedicated "Pure Offline" Mode Toggle:** Added a persistent offline flight mode switch in Settings and an active alert banner on Home (`✈️ PURE OFFLINE MODE ACTIVE`). When active, search and recommendations query strictly locally stored and cached tracks with zero remote network calls.
- **Download Task Cancellation:** Interactive cancel button (`✕`) for active or queued download tasks directly in the Downloads tab, immediately aborting network streaming and clearing temporary files.

### Fixed
- **Mid-Playback Song Selection Position Carryover:** Fixed bug where selecting a new song while another song was playing in the middle caused the new song to start at the previous song's elapsed time instead of 0:00. Now halts previous playback and resets progress immediately upon selection.
- **Song Selection Pause Glitch:** Fixed bug where selecting a song would resolve the stream and then enter a paused state, requiring a second tap on the play button. `playWhenReady` is now explicitly set to `true` upon track selection.
- **Continuous Play & Radio Provider Switching:** Fixed bug where starting a YouTube song in Auto (Multi-Source) mode switched to JioSaavn for subsequent radio songs. Radio recommendation routing now respects the seed song's actual source provider (YouTube stays on YouTube, JioSaavn on JioSaavn, SoundCloud on SoundCloud) with multi-source failover.
- **YouTube Video ID Format Matching:** Corrected regex detection for 11-character base64url YouTube IDs that contain underscores (`_`) or hyphens (`-`), preventing accidental fallback to JioSaavn stream queries.
- **Queue Pre-Filling & Upcoming Songs:** Fixed bug where the upcoming song queue failed to pre-populate. Proactively pre-fetches and fills upcoming suggestions whenever 3 or fewer tracks remain in the queue and eagerly populates when the Queue bottom sheet is opened.
- **Dynamic Cache Evictor Infinite Loop:** Fixed potential deadlock and infinite loop in `DynamicCacheEvictor` when span removal failed due to locked cache files.
- **Jam Ghost Participants & Vote-Skip Threshold:** Added Firebase `.onDisconnect().removeValue()` to participant references so disconnected listeners are automatically removed and do not block 50% vote-skip consensus.
- **OkHttp Socket Descriptor Leak:** Wrapped download HTTP execution in `TrackDownloadManager` with `response.use { }` to prevent connection leaks during track downloads.
- **HttpURLConnection Descriptor Leaks:** Enforced safe stream reading and `.disconnect()` calls across JioSaavn stream resolution.

## [2.6.1] - 2026-10-21
### Fixed
- **Mid-Playback Song Selection Position Carryover:** Fixed bug where selecting a new song while another song was playing in the middle caused the new song to start at the previous song's elapsed time instead of 0:00. Now halts previous playback and resets progress immediately upon selection.
- **Song Selection Pause Glitch:** Fixed bug where selecting a song would resolve the stream and then enter a paused state, requiring a second tap on the play button. `playWhenReady` is now explicitly set to `true` upon track selection.
- **Continuous Play & Radio Provider Switching:** Fixed bug where starting a YouTube song in Auto (Multi-Source) mode switched to JioSaavn for subsequent radio songs. Radio recommendation routing now respects the seed song's actual source provider (YouTube stays on YouTube, JioSaavn on JioSaavn, SoundCloud on SoundCloud) with multi-source failover.
- **YouTube Video ID Format Matching:** Corrected regex detection for 11-character base64url YouTube IDs that contain underscores (`_`) or hyphens (`-`), preventing accidental fallback to JioSaavn stream queries.
- **Queue Pre-Filling & Upcoming Songs:** Fixed bug where the upcoming song queue failed to pre-populate. Proactively pre-fetches and fills upcoming suggestions whenever 3 or fewer tracks remain in the queue and eagerly populates when the Queue bottom sheet is opened.
- **Dynamic Cache Evictor Infinite Loop:** Fixed potential deadlock and infinite loop in `DynamicCacheEvictor` when span removal failed due to locked cache files.
- **Jam Ghost Participants & Vote-Skip Threshold:** Added Firebase `.onDisconnect().removeValue()` to participant references so disconnected listeners are automatically removed and do not block 50% vote-skip consensus.
- **OkHttp Socket Descriptor Leak:** Wrapped download HTTP execution in `TrackDownloadManager` with `response.use { }` to prevent connection leaks during track downloads.
- **HttpURLConnection Descriptor Leaks:** Enforced safe stream reading and `.disconnect()` calls across JioSaavn stream resolution.

### Added
- **Multi-Line Synchronized Lyrics Teleprompter:** Upgraded the Lyrics Bottom Sheet from a raw LRC dump into a full interactive, auto-scrolling teleprompter with active sung line highlighting in glowing amber and click-to-seek functionality.
- **Workspace Agent Skills:** Integrated specialized skills for Media3 ExoPlayer streaming, Jetpack Compose UI, Clean Architecture, and GitHub Actions CI/CD.

## [2.6.0] - 2026-10-18
### Added
- **Global Track Context Menu & "Add to Playlist" Dialog:** Seamlessly add any track from Search, Home, or Library directly into custom playlists via the new interactive dialog.
- **In-Crate Playlist Track Management:** Delete or add tracks dynamically within custom playlists with real-time UI refresh.
- **Language-Aware Radio Recommendation Engine:** Smart regional language affinity detection (Malayalam, Tamil, Hindi, English, Telugu, Punjabi) ensures radio recommendations closely match the seed song's language and vibe without jarring linguistic or genre shifts.
- **Artist Diversity Guard:** Radio and suggestion queues now cap same-artist tracks to a balanced threshold (≤ 3 tracks), preventing monotone artist flooding.
- **Screen-Off Continuous Playback (WakeLocks):** Added partial CPU wake locks and Wi-Fi locks to `MusicPlaybackService` to ensure smooth, uninterrupted continuous playback even when the screen is turned off or the device enters Doze mode.
- **Manual Jam Room Resync:** Added an explicit quick re-sync control in `JamRoomScreen` allowing participants to instantly synchronize with the host on variable-latency connections.

### Fixed
- **Autoplay Stopping Bug:** Resolved an issue where playback would stop after a single song when playing Spotify-imported playlists or album queues. Playback now smoothly advances through the queue without interruptions.
- **Favorite / Love Button Flashing:** Eliminated the bug where pressing the heart button caused the icon to flash amber and revert due to background player progress polling overwriting reactive Room DB state.
- **JAM Playback Activation & Sync:** Resolved buffer readiness race conditions where participants joined a room but playback failed to trigger or seeked continuously.
- **Shuffle & Repeat Mode Inconsistencies:** Refactored shuffle queue handling and repeat state transitions to prevent single-track infinite loops.
- **Playlist 3-Dot Action Menus:** Fixed non-responsive 3-dot menus on song rows across all screens.

### Refactored
- **Codebase Clean Architecture & Deduplication:** Completely pruned over 285 lines of duplicate and unused imports across 28 Kotlin files, ViewModels, and unit tests.
- **Manifest Cleanup:** Removed obsolete non-existent `SpotifyLoginActivity` declaration from `AndroidManifest.xml`.
- **Build Configurations:** Streamlined `app/build.gradle.kts` and cleaned obsolete template comments.

## [2.5.2] - 2026-10-09
### Fixed
- **JAM Playback Synchronization:** Completely eliminated race conditions and seek loops when participants sync to the host.
- **Readiness-Aware Sync:** Resolved bugs where position corrections were applied before the stream had finished buffering, causing tracks to restart.
- **Pause Override Fix:** Fixed a bug where joining a paused room would cause the participant's device to aggressively unpause when the stream resolved.
- **Drift Correction Smoothing:** Throttled timestamp correction to run conservatively instead of spamming position requests during normal playback.

## [2.5.1] - 2026-10-08
### Fixed
- **Offline Crash:** Resolved a critical bug where launching the app without an internet connection caused an immediate crash due to unhandled exceptions in the trending music flow.

## [2.5.0] - 2026-10-04
### Added
- **Pulse JAM (Live Sync Rooms):** Create and join live music rooms! Features a fully synchronized Firebase Realtime Queue where everyone in the room listens to the exact same music in perfect sync.
- **In-Room Search & Queueing:** Tap '+ ADD SONG' inside any Jam Room to dynamically search for tracks and instantly broadcast them to the global Room Queue.
- **Autoplay Suggestion Bridging:** When the Room Host reaches the end of the queue, the Music Engine automatically generates smart suggestions and seamlessly pushes them directly into the Firebase Room Queue for everyone to enjoy.
- **Host Room Controls:** Hosts can kick participants and effortlessly control the playback state for all listeners.
- **One-Tap Room Invites:** Simply tap the massive 6-digit Room Code at the top of the Jam Room to copy it to your clipboard.
- **Democratic Vote Skipping:** Guests can trigger `VOTE SKIP`, automatically advancing tracks once a 50% room consensus is reached.
- **Host Queue Controls & Force Skip:** Hosts can delete individual songs from the upcoming queue and immediately force-skip tracks.
- **Timestamp Drift Correction:** Listeners automatically adjust playback positions when drift exceeds 3 seconds relative to the host's timestamp broadcast.
- **Host Kick Detection:** Listeners are notified and playback halts gracefully if removed from a room.

### Changed
- **Scroll Handling Refactor:** Completely re-engineered the `SkeuoTactileButton` touch processing to perfectly decouple drag gestures, resulting in flawless scrolling inside large queues and participant lists.
- **Player Controller Routing:** The core `MusicPlayerController` now elegantly intercepts and replaces its internal ExoPlayer queue with the active Firebase Room Queue the moment a Room song is played.


## [2.1.4] - 2026-10-02
### Added
- **Auto-Scrolling Time-Synced Lyrics:** Beautiful Neumorphic animated lyrics injected right into the main player UI, automatically syncing and scrolling with the track.
- **Sleep Timer:** Securely handles pausing playback and shutting down the music service after a customizable period (15m, 30m, 60m, 120m).
- **Poppins Typography Upgrade:** Replaced the system font with the premium geometric sans-serif font *Poppins*, enhancing readability and complementing the UI style.

### Changed
- **Neumorphism Design Pass:** Significantly increased padding and margin whitespace across the `HomeScreen` and `FullScreenPlayerScreen` to prevent Neumorphic shadows from bleeding into each other.
- **Tactile Button Animations:** Upgraded `SkeuoTactileButton` to utilize physical-feeling inset animation logic via `animateDpAsState` when pressed.
- **Color Accent Revision:** Restored the core color palette to use the signature Amber/Yellow accent.

### Removed
- **Spotify Integration:** Completely ripped out the `SpotifyAuthManager` and `SpotifyApiService` to remove rate limits, app lag, and backend conflicts. Playlists are now imported safely via web-scraping instead.

