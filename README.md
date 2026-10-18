# Pulse Music 🎵

<div align="center">
  <img src="https://raw.githubusercontent.com/chriz-3656/Pulse-Music/main/app/src/main/res/drawable/app_icon.png" width="140" height="140" alt="Pulse Music Logo" style="border-radius: 28px; box-shadow: 0 8px 24px rgba(0,0,0,0.3);">
  <h1>Pulse Music</h1>
  <h3><em>Music beyond silence.</em></h3>
  <p><strong>A high-fidelity, aesthetically driven Android music streaming & offline player crafted with Jetpack Compose, featuring an analog Skeuomorphic & Yellow Neumorphic hardware design language.</strong></p>

  <p>
    <a href="https://github.com/chriz-3656/Pulse-Music/releases"><img src="https://img.shields.io/badge/Release-v2.6.0-FFC107?style=for-the-badge&logo=android&logoColor=black" alt="Version 2.6.0"></a>
    <a href="https://github.com/chriz-3656/Pulse-Music/actions"><img src="https://img.shields.io/badge/CI-Passing-28a745?style=for-the-badge&logo=githubactions&logoColor=white" alt="CI Status"></a>
    <img src="https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-202124?style=for-the-badge&logo=android&logoColor=FFC107" alt="Platform">
    <img src="https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
    <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
    <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge" alt="License"></a>
  </p>
</div>

---

## 📑 Table of Contents

- [Overview](#-overview)
- [✨ Key Features](#-key-features)
  - [🎧 Pulse JAM — Real-Time Live Sync Rooms](#-pulse-jam--real-time-live-sync-rooms)
  - [📻 Smart Radio Engine & Language Affinity](#-smart-radio-engine--language-affinity)
  - [⚡ Continuous Autoplay & Screen-Off Playback](#-continuous-autoplay--screen-off-playback)
  - [🎛️ Tactile Skeuomorphic & Neumorphic Design](#️-tactile-skeuomorphic--neumorphic-design)
  - [📜 Time-Synced Scrolling Lyrics](#-time-synced-scrolling-lyrics)
  - [📁 Playlist & Library Management](#-playlist--library-management)
  - [💾 High-Fidelity Audio & Offline Downloads](#-high-fidelity-audio--offline-downloads)
  - [⏱️ Sleep Timer & In-App Updater](#️-sleep-timer--in-app-updater)
- [🏗️ Architecture & Technology Stack](#️-architecture--technology-stack)
- [📂 Project Structure](#-project-structure)
- [🚀 Getting Started & Building](#-getting-started--building)
  - [Prerequisites](#prerequisites)
  - [Clone & Build](#clone--build)
  - [Cloud CI / CD Pipeline](#cloud-ci--cd-pipeline)
- [📥 Downloading the App](#-downloading-the-app)
- [📜 Versioning & Changelog](#-versioning--changelog)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)

---

## 🌟 Overview

**Pulse Music** reimagines personal audio playback on mobile devices. While contemporary music players have converged onto flat, sterile design systems, Pulse Music pairs state-of-the-art streaming technology with a tactile, analog hardware aesthetic inspired by vintage studio decks, reel-to-reel tape consoles, and mechanical audio equipment.

Featuring deep acoustic amber lighting, authentic beveled decks, reactive VU meter needle movements, and physical spring-action buttons, Pulse Music treats every song interaction as a tactile experience—all backed by seamless background streaming, synchronized lyrics, multi-provider routing, and live multi-device room synchronization.

---

## ✨ Key Features

### 🎧 Pulse JAM — Real-Time Live Sync Rooms
*Listen together in perfect harmony, anywhere in the world.*
- **Zero-Drift Synchronized Listening:** Host or join live music rooms powered by Firebase Realtime Database. All participants hear the exact same audio in sub-second sync.
- **Buffer-Aware Playback Activation:** Streams pre-buffer and trigger seamlessly upon host playback without stutter or desynchronization.
- **In-Room Search & Dynamic Queueing:** Anyone in the room can tap `+ ADD SONG` to search online catalogs and append songs directly into the shared room queue.
- **Democratic Vote Skipping:** Listeners can trigger vote-skips; reaching a 50% room majority immediately advances to the next track.
- **Manual Quick Re-sync:** Instantly align playback timestamps on unpredictable cellular networks with a dedicated manual sync button.
- **Autoplay Suggestion Bridging:** Once the room queue finishes, the smart recommendation engine automatically fills the queue with similar music so the party never stops.

### 📻 Smart Radio Engine & Language Affinity
*Music recommendations that respect language, mood, and artist balance.*
- **Regional Language Detection:** The radio engine identifies the native language of the seed track (Malayalam, Tamil, Hindi, English, Punjabi, Telugu, etc.) and keeps recommendations strictly within related linguistic and cultural contexts.
- **Artist Diversity Guard:** Never get stuck in an endless loop of a single artist. The queue algorithm caps same-artist tracks to a balanced threshold (≤ 3 tracks).
- **Affinity-Ranked Queueing:** Top-ranked similarity matches play first, preserving the natural musical flow of the session.

### ⚡ Continuous Autoplay & Screen-Off Playback
*Uninterrupted, smooth music from first note to last.*
- **Screen-Off WakeLocks:** Foreground service utilizes dedicated CPU `WakeLock` and `WifiLock` guards, completely eliminating audio cutoffs when Android enters Doze or sleep mode.
- **Continuous Spotify Playlist Autoplay:** Seamlessly play full imported playlists or album queues with zero mid-queue pauses.
- **Intelligent Pre-buffering:** High-speed ExoPlayer pipelines ensure instant transitions between consecutive songs.

### 🎛️ Tactile Skeuomorphic & Neumorphic Design
*Audio hardware you can feel beneath your fingertips.*
- **Physical Spring Buttons:** `SkeuoTactileButton` elements simulate mechanical depression depth via animated inset elevations.
- **Analog VU Meter:** Real-time animated needle meter reacting dynamically to audio levels and playback energy.
- **Backlit LCD Screens & LED Glow:** Glowing amber indicators, metallic knurled knobs, and recessed display bezels.
- **Poppins Typography:** Premium geometric sans-serif typography tailored for high legibility in dark environments.

### 📜 Time-Synced Scrolling Lyrics
*Sing along with millisecond precision.*
- **Auto-Scrolling Teleprompter:** Synchronized LRC lyrics highlight and scroll smoothly in real time as the vocal line is sung.
- **Integrated Player Sheet:** Swipeable bottom sheet player view lets you transition between album artwork, track queue, and lyrics instantly.

### 📁 Playlist & Library Management
*Complete control over your audio collection.*
- **Global Context Menus (3-Dot):** Tap the 3-dot menu on any song row across Home, Search, or Library to trigger actions.
- **"Add to Playlist" Dialog:** Quickly add any song to an existing playlist or create a new playlist on the fly.
- **In-Crate Track Deletion:** Remove songs directly from custom playlists with instant reactive Room DB updates.
- **Instant Reactive Favorite Toggle:** Loved songs update instantaneously across Room cache, notification shade, and player view.
- **Spotify Web-Import:** Import public Spotify playlists directly via clean scraping without needing developer API credentials.

### 💾 High-Fidelity Audio & Offline Downloads
*Audiophile stream quality, everywhere you go.*
- **Multi-Source Routing:** Aggregates streams across JioSaavn (up to 320kbps HD) and YouTube Music (via NewPipe engine) with automatic fallback.
- **Offline Storage:** Save tracks locally with ID3 metadata and album art cached for full offline playback.
- **Offline Priority:** Automatically prioritizes locally downloaded files over cellular streams when offline.

### ⏱️ Sleep Timer & In-App Updater
- **Smart Sleep Timer:** Set countdown timers (15m, 30m, 45m, 60m, 120m) that gently fade out audio and stop the background service.
- **Direct GitHub Releases Updater:** Automatically checks GitHub for new APK releases and downloads updates directly inside the app.

---

## 🏗️ Architecture & Technology Stack

Pulse Music is built following **Clean Architecture** and **Modern Android Development (MAD)** best practices:

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                     │
│  Jetpack Compose • Material 3 • Navigation • Custom Skeuo   │
│                 StateFlow • ViewModel Lifecycle             │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                         Domain Layer                        │
│            Use Cases • Domain Models • Repository Interfaces│
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                          Data Layer                         │
│  Media3 ExoPlayer • Room Database • Retrofit & OkHttp       │
│  Firebase Realtime DB (JAM) • NewPipe Extractor • Coil      │
└─────────────────────────────────────────────────────────────┘
```

| Layer | Technologies |
|---|---|
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 & Custom Neumorphic Canvas Drawers |
| **Audio Core** | [AndroidX Media3 ExoPlayer](https://developer.android.com/guide/topics/media/media3) with Foreground Service & MediaSession |
| **Concurrency** | Kotlin Coroutines & `StateFlow` / `SharedFlow` |
| **Local Cache** | [Room Database](https://developer.android.com/training/data-storage/room) with SQLite & KSP Codegen |
| **Realtime Sync** | [Firebase Realtime Database](https://firebase.google.com/products/realtime-database) & Firebase Auth |
| **Networking** | [OkHttp3](https://square.github.io/okhttp/), [Retrofit](https://square.github.io/retrofit/), [Moshi](https://github.com/square/moshi) |
| **Streaming Engines**| NewPipe Extractor, JioSaavn Native Client, SoundCloud Public API |
| **Image Loading** | [Coil](https://coil-kt.github.io/coil/) |

---

## 📂 Project Structure

```text
Pulse-Music/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/                 # Data Layer: Room DB, Remote Clients, Repositories
│   │   │   │   │   ├── local/            # Room DAOs, Entities, Database
│   │   │   │   │   ├── player/           # Cache Evictors, Download Manager
│   │   │   │   │   ├── remote/           # Retrofit APIs, JioSaavn, SoundCloud, NewPipe
│   │   │   │   │   └── repository/       # MusicRepositoryImpl, JamSessionManager
│   │   │   │   ├── di/                   # Manual Dependency Injection (AppContainer)
│   │   │   │   ├── domain/               # Domain Layer: Models, Use Cases, Interfaces
│   │   │   │   ├── player/               # MusicPlayerController (ExoPlayer orchestration)
│   │   │   │   ├── service/              # MusicPlaybackService (Media3 Foreground Service)
│   │   │   │   ├── ui/                   # Presentation Layer: Compose Screens & Components
│   │   │   │   │   ├── components/       # Skeuomorphic Buttons, VU Meter, Lyrics, Dialogs
│   │   │   │   │   ├── screens/          # Home, Player, Library, Search, JAM, Album, Artist
│   │   │   │   │   ├── theme/            # Neumorphic Color Palettes, Typography, Spacing
│   │   │   │   │   └── viewmodel/        # Architecture ViewModels & UI States
│   │   │   │   └── util/                 # AppUpdater, Logger, Helpers
│   │   │   ├── res/                      # Android Resources (Fonts, Drawables, Mipmaps)
│   │   │   └── AndroidManifest.xml
│   │   └── test/                         # JVM Unit Tests (Robolectric & Coroutines Test)
│   └── build.gradle.kts                  # App-level build configuration (v2.6.0)
├── gradle/                               # Gradle Wrapper & Version Catalog
│   └── libs.versions.toml
├── .github/workflows/ci.yml              # Cloud CI/CD Automated Build & Release Pipeline
├── CHANGELOG.md                          # Version History & Release Notes
├── CONTRIBUTING.md                       # Contribution Guidelines
├── LICENSE                               # GNU General Public License v3.0
└── README.md                             # Project Documentation
```

---

## 🚀 Getting Started & Building

### Prerequisites
- **Android Studio:** Ladybug (2024.2+) or newer
- **JDK:** Java 21 (Temurin or OpenJDK recommended)
- **Android SDK:** Compile SDK 36, Target SDK 36, Minimum SDK 24 (Android 7.0+)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/chriz-3656/Pulse-Music.git
cd Pulse-Music

# Run unit test suite
./gradlew :app:testDebugUnitTest

# Assemble Debug APK
./gradlew :app:assembleDebug
```

Output APK will be generated at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

### Cloud CI / CD Pipeline
Pulse Music includes an automated **GitHub Actions CI/CD Pipeline** (`.github/workflows/ci.yml`). Every push and pull request triggers:
1. JVM unit test execution across all player controller, radio, and offline caching test suites.
2. Complete release and debug APK assembly with signing configuration.
3. Automated artifact packaging (`pulse-music-apks.zip`) uploaded directly to workflow runs.
4. Automatic GitHub Release publishing upon tag creation (`v*`).

---

## 📥 Downloading the App

1. Visit the [GitHub Releases](https://github.com/chriz-3656/Pulse-Music/releases) page for stable release APKs.
2. Or download the latest continuous build from the [GitHub Actions Artifacts](https://github.com/chriz-3656/Pulse-Music/actions) (`pulse-music-apks`).
3. Install the APK on your Android device (ensure "Install unknown apps" is granted in settings).

---

## 📜 Versioning & Changelog

Pulse Music follows [Semantic Versioning](https://semver.org/). See the full release notes and change logs in [CHANGELOG.md](CHANGELOG.md).

- **Current Stable Version:** `v2.6.0` (2026-10-18)
- **Highlights:** Pulse JAM playback activation & manual sync, continuous background autoplay with WakeLocks, language-aware radio recommendation engine, 3-dot playlist management, favorite love button fix, and full codebase deduplication.

---

## 🤝 Contributing

Contributions, issues, and feature proposals are warmly welcome!
- Check out [CONTRIBUTING.md](CONTRIBUTING.md) for full guidelines, branch naming conventions, and testing instructions.
- All contributors must abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for complete details.

---

<div align="center">
  <p>Crafted with ❤️ by <a href="https://github.com/chriz-3656">Chris Mon Saji</a> and Open Source Contributors.</p>
</div>
