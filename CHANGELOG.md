# 📋 Changelog

All notable changes to the **Pulse Music** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [1.2.0] - 2026-09-18

### Added
- In-App Auto Updater to automatically fetch and install new releases from GitHub.
- Permanent Keystore Configuration for conflict-free seamless upgrades

### Planned
- Audio visualizer with FFT real-time spectrum analysis.
- Gapless playback and crossfade transitions between tracks.
- Backup and restore of user playlists via JSON file export/import.

---

## [1.1.0] - 2026-09-17

### ⚡ Pulse Wave Redesign & Dynamic Engine Enhancements

### Added
- **Pulse Wave Brand Identity & Adaptive Launcher Icon**:
  - Re-engineered adaptive app icon (`ic_launcher_foreground.xml` & `ic_launcher_background.xml`) featuring a high-contrast electric yellow waveform with acoustic radial harmonics on a deep pitch-black chassis.
  - Redesigned in-app vector logo component (`SkeuoAppLogo`) featuring dynamic pulse waveforms, equalizer spectrum depth bars, and white-hot spark nodes across the Home and Settings screens.
- **Dynamic Radio Recommendations & Queue Engine**:
  - Rebuilt search track playback workflow: selecting a song immediately plays that track while dynamically queueing related algorithmic tracks and radio recommendations, preventing duplicate static search lists from clogging the queue.
  - Implemented continuous playback toggle in Settings, connecting track completion triggers directly to auto-queue suggestions.

### Changed
- **Zero Hardcoded Content**:
  - Audited codebase and verified 100% dynamic API resolution for all songs, artists, albums, and playlists across streaming providers.
  - Cleaned redundant repository scaffolding and temporary web assets for a lightweight, optimized Android project structure.

### Fixed
- Fixed search playback flow so contextual actions (**Play Next**, **Add to Queue**, **Start Radio**) accurately modulate the live play queue without disrupting the active playback state.

---

## [1.0.0] - 2026-09-16

### 🚀 Initial Release

Welcome to the initial production release of **Pulse Music**, a Hi-Fi Android audio streaming & offline playback application.

### Added
- **Core Playback Engine**:
  - AndroidX Media3 ExoPlayer integration embedded within a sticky `MusicPlaybackService` foreground service.
  - Seamless background audio streaming with Android system notification controls, lockscreen media controls, and Bluetooth remote transport event handling.
  - Dynamic audio quality selector supporting 320 kbps (High Fidelity) and 160 kbps (Standard) with automatic stream resolution fallback.
  - In-memory and disk LRU chunk caching via `MediaCacheManager` to minimize re-buffering on volatile cellular networks.

- **Offline Download System**:
  - `TrackDownloadManager` implementing direct OkHttp chunked streaming to private app storage.
  - Full offline priority: local tracks are served directly from disk URIs without network dependency, enabling complete airplane mode playback.
  - Library **Downloads** tab with offline-only playback queue and disk storage removal tools.

- **Search & Provider Aggregation**:
  - Multi-source search engine aggregating JioSaavn, SoundCloud, and YouTube Music streams.
  - Instant search interface with debounced typing, query history, and trending track queries.
  - Dedicated Artist and Playlist detail views displaying discography, top tracks, and related artists.

- **Lyrics & Queue Management**:
  - Real-time synchronized lyrics renderer with active line highlight and auto-scroll tracking.
  - Plain lyrics toggle for non-synced tracks with typography scaling.
  - Fullscreen expandable queue drawer supporting track removal, drag reordering, shuffle, and repeat modes.

- **Tactile UI & Skeuomorphic Design**:
  - Handcrafted dark theme with vacuum-tube warm amber accents (`#FF9500`), brushed metal surfaces, and knurled knobs.
  - Interactive vinyl turntable with realistic rotational speed and needle-drop animations.
  - Edge-to-Edge window inset handling conforming to modern Android 15 design standards.

- **Local Persistence & Settings**:
  - Local Room database with KSP for playlists, favorites, song metadata caching, and playback history.
  - User preferences store for default streaming quality, cache size limits, and playback resume.

### Fixed
- Fixed SoundCloud and JioSaavn audio streaming 403 Forbidden errors by configuring appropriate `User-Agent`, `Referer`, and `Origin` headers in the OkHttp network interceptor pipeline.
- Fixed lockscreen media notification controls to correctly reflect play, pause, next, and previous actions on Android 13 and 14+.
- Fixed offline track playback routing to check local storage before triggering network queries.

### Security & Quality
- Configured compile SDK 35 and Java 17 toolchain.
- Created automated JVM unit test suites covering offline file priority and audio resolution mechanics.
