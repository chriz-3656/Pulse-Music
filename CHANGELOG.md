# 📋 Changelog

All notable changes to the **Pulse Music** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- Audio visualizer with FFT real-time spectrum analysis.
- Gapless playback and crossfade transitions between tracks.
- Backup and restore of user playlists via JSON file export/import.

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
