# 🎵 Pulse Music

<p align="center">
  <img src="app/src/main/res/drawable/app_icon.png" width="128" height="128" alt="Pulse Music Icon" />
</p>

<p align="center">
  <strong>Hi-Fi Android Music Streaming & Offline Player</strong><br>
  Built with Jetpack Compose, AndroidX Media3 (ExoPlayer), Room Database, and Modern Skeuomorphic Craftsmanship.
</p>

<p align="center">
  <a href="#features"><img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Platform: Android" /></a>
  <a href="#tech-stack"><img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Language: Kotlin" /></a>
  <a href="#tech-stack"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
  <a href="#playback-engine"><img src="https://img.shields.io/badge/Engine-AndroidX%20Media3%20ExoPlayer-FF6F00" alt="AndroidX Media3" /></a>
  <a href="CHANGELOG.md"><img src="https://img.shields.io/badge/Version-1.1.0-FFD600?logo=android" alt="Version: 1.1.0" /></a>
  <a href="#license"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License: Apache 2.0" /></a>
  <a href="#architecture"><img src="https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-brightgreen" alt="Architecture" /></a>
</p>

---

## 📖 Overview

**Pulse Music** is an open-source, production-grade Android music streaming application designed for audio purists and music enthusiasts. It blends the warmth of tactile vintage hardware—warm vacuum-tube amber glows, textured faceplates, analog VU meters, and turntable vinyl physics—with cutting-edge Android architecture.

Stream high-bitrate audio from decentralized providers (SoundCloud, JioSaavn, and YouTube Music fallback), download full tracks for zero-latency offline playback, view synchronized lyrics, create local playlists, and manage playback through an AndroidX Media3 foreground service with system notification and lockscreen controls.

---

## ✨ Key Features

### 🎧 Playback & Audio Engine
- **AndroidX Media3 ExoPlayer Foreground Service**: Persistent audio playback that survives app minimization, system memory pressure, and screen lock.
- **High-Fidelity Audio Streams**: Adaptive 320 kbps and 160 kbps streaming with dynamic fallback.
- **Hardware Media Controls**: Full integration with Bluetooth audio devices, headphone remotes, Android Lockscreen Media Controls, and MediaSession metadata.
- **Audio Cache & Buffer Pipeline**: Pre-fetching and LRU chunk caching with `MediaCacheManager` for stutter-free playback even on fluctuating networks.

### 💾 Offline Downloads & Storage
- **Atomic Background Downloader**: Dedicated `TrackDownloadManager` with OkHttp streaming pipeline and direct Room database synchronization.
- **Instant Offline Priority**: Playback engine automatically prioritizes locally downloaded audio files—zero network latency and full offline support in airplane mode.
- **Offline Library Management**: Dedicated Downloads tab in the Library screen to browse, play offline queues, and manage storage.

### 🔍 Search & Multi-Provider Aggregation
- **Federated Stream Aggregator**: Query tracks, artists, albums, and playlists simultaneously across JioSaavn, SoundCloud, and YouTube Music.
- **Smart Queue & Radio Suggestions**: Selecting any search result immediately plays that song while automatically cueing algorithmic recommendations and radio suggestions without flooding the queue with static search listings.
- **Debounced Instant Search**: Instant typeahead results with query history and trending suggestions.
- **Artist & Playlist Deep-Dives**: Explore full artist discographies, top songs, related artists, and curated community playlists.

### 📜 Synchronized Lyrics & Queue Management
- **Time-Synced & Plain Lyrics**: Real-time karaoke-style synchronized lyrics with smooth auto-scrolling and manual timestamp scrubbing.
- **Dynamic Playback Queue**: Drag-and-drop track reordering, shuffle mode, repeat-all, and repeat-one toggle modes with continuous autoplay streaming.

### 🎛️ Tactile Hi-Fi & Skeuomorphic UI
- **Pulse Wave Brand Identity**: High-voltage electric yellow pulse waveform icon (`#FFD600`) framed over a pitch-black background with acoustic radial harmonics.
- **Analog Aesthetic**: Warm amber glow (`#FF9500`), brushed dark metallic panels, knurled dials, and dynamic VU meters.
- **Vinyl Turntable Deck**: Rotating vinyl platter with realistic needle-drop states and album art label reproduction.
- **Modern Material 3 Foundation**: Full support for Edge-to-Edge display, gesture navigation, and Android 15 window insets.

---

## 🏛️ Architecture & Tech Stack

Pulse Music follows **Clean Architecture** and the **MVVM (Model-View-ViewModel)** design pattern with unidirectional data flow (UDF).

```
┌─────────────────────────────────────────────────────────────┐
│                       UI Layer (Compose)                    │
│   Screens (Home, Search, Library, FullPlayer, Artist, etc.) │
│   Components (VinylRecord, MiniPlayer, SkeuoCards, Dials)   │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow / Events
┌──────────────────────────────▼──────────────────────────────┐
│                      ViewModel Layer                        │
│   HomeViewModel, SearchViewModel, LibraryViewModel, etc.    │
└──────────────────────────────┬──────────────────────────────┘
                               │ Use Cases
┌──────────────────────────────▼──────────────────────────────┐
│                       Domain Layer                          │
│   UseCases (SearchMusic, ManageDownloads, StreamTrack, etc.)│
│   Repository Interfaces & Domain Entities (Song, Playlist)  │
└──────────────────────────────┬──────────────────────────────┘
                               │ Repository Implementation
┌──────────────────────────────▼──────────────────────────────┐
│                        Data Layer                           │
│   Local: Room DB (MusicDao, SongEntity, PlaylistEntity)     │
│   Remote: OkHttp, Retrofit, JioSaavn & SoundCloud APIs      │
│   Service: MusicPlaybackService (AndroidX Media3 ExoPlayer) │
│   Cache: MediaCacheManager, TrackDownloadManager            │
└─────────────────────────────────────────────────────────────┘
```

### Core Libraries & Technologies

| Layer | Technologies |
|---|---|
| **Language** | [Kotlin 2.0+](https://kotlinlang.org/) |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material Design 3](https://m3.material.io/) |
| **Audio Engine** | [AndroidX Media3](https://developer.android.com/media/media3) (ExoPlayer 1.4+, MediaSession, MediaNotification) |
| **Local Persistence** | [Room Database](https://developer.android.com/training/data-storage/room) with [KSP](https://kotlinlang.org/docs/ksp-overview.html) |
| **Asynchronous** | [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines) & [StateFlow / SharedFlow](https://developer.android.com/kotlin/flow) |
| **Networking** | [OkHttp 4](https://square.github.io/okhttp/) & [Retrofit](https://square.github.io/retrofit/) |
| **Image Loading** | [Coil 2](https://coil-kt.github.io/coil/) (Compose integration, disk caching) |
| **Serialization** | [Kotlinx Serialization JSON](https://github.com/Kotlin/kotlinx.serialization) |

---

## 📂 Project Structure

```
pulse-music/
├── .github/
│   ├── workflows/             # GitHub Actions CI/CD pipelines
│   ├── ISSUE_TEMPLATE/        # Bug reports & feature request templates
│   └── PULL_REQUEST_TEMPLATE.md
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/             # Local database, entities, DTOs, API services, caches
│   │   │   │   │   ├── local/        # Room Database & DAOs
│   │   │   │   │   ├── player/       # ExoPlayer controller, download & cache managers
│   │   │   │   │   ├── remote/       # Remote data sources & REST API clients
│   │   │   │   │   └── repository/   # Repository implementations
│   │   │   │   ├── di/               # Manual Dependency Injection (AppContainer)
│   │   │   │   ├── domain/           # Domain models, repository interfaces & use cases
│   │   │   │   ├── service/          # Media3 Foreground Playback Service
│   │   │   │   └── ui/               # Jetpack Compose UI
│   │   │   │       ├── components/   # Reusable UI widgets, buttons, dials, rows
│   │   │   │       ├── navigation/   # Compose Navigation & routes
│   │   │   │       ├── screens/      # Feature screens (Home, Search, Library, Player, etc.)
│   │   │   │       ├── theme/        # Color schemes, typography, skeuomorphic styles
│   │   │   │       └── viewmodel/    # MVVM ViewModels
│   │   │   └── res/                  # Drawables, strings, colors, adaptive icons
│   │   └── test/                     # Local JVM unit tests
│   └── build.gradle.kts              # Application build configuration
├── gradle/                           # Gradle wrapper & version catalog (libs.versions.toml)
├── CHANGELOG.md                      # Release notes & version changelog
├── CONTRIBUTING.md                   # Contribution guide & coding standards
├── CODE_OF_CONDUCT.md                # Community standards & Contributor Covenant
├── LICENSE                           # Apache License 2.0
├── SECURITY.md                       # Security vulnerability reporting policy
└── settings.gradle.kts               # Project root build settings
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug (2024.2+) or newer
- **JDK**: Java 17 or higher
- **Android SDK**: Compile SDK `35`, Min SDK `24`
- **Gradle**: 8.7+ (configured via Gradle Version Catalog)

### Clone & Build

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/pulse-music.git
   cd pulse-music
   ```

2. Open the project in Android Studio, or build via command line:
   ```bash
   gradle :app:assembleDebug
   ```

3. Run JVM unit tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```

4. Install the debug APK onto a connected Android device or emulator:
   ```bash
   gradle :app:installDebug
   ```

---

## 🔒 Permissions & Security

Pulse Music adheres strictly to the principle of least privilege:

| Permission | Usage |
|---|---|
| `INTERNET` | Stream online tracks, download album art, and fetch synchronized lyrics |
| `ACCESS_NETWORK_STATE` | Detect network transitions to gracefully toggle offline/online streaming |
| `FOREGROUND_SERVICE` | Ensure uninterrupted background playback when app is minimized |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Android 14+ required permission for media playback services |
| `POST_NOTIFICATIONS` | Display rich playback controls and artwork in the Android Notification drawer (requested at runtime on Android 13+) |
| `WAKE_LOCK` | Prevent CPU sleep while actively decoding and rendering audio buffers |

---

## 🧪 Testing

The codebase incorporates automated unit testing focusing on core Critical User Journeys (CUJs):

- **Offline Playback & Local Path Resolution**: Verifies that tracks cached to disk are served immediately from local URIs without touching network layers.
- **Audio Quality Switching**: Tests fallback behaviors between 320 kbps, 160 kbps, and remote stream providers.
- **Data Serialization & Entity Mapping**: Tests data transformation across network DTOs, Room Entities, and Domain Models.

To run all unit tests:
```bash
gradle :app:testDebugUnitTest
```

---

## 🤝 Contributing

We welcome contributions of all kinds—bug fixes, new features, UI refinements, and documentation improvements!

Please read our [Contributing Guide (CONTRIBUTING.md)](CONTRIBUTING.md) and [Code of Conduct (CODE_OF_CONDUCT.md)](CODE_OF_CONDUCT.md) before submitting pull requests.

---

## 📄 License

Pulse Music is licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for complete details.

```
Copyright 2026 Pulse Music Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
