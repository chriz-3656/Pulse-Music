# Pulse Music v2.5.2

<div align="center">
  <img src="https://raw.githubusercontent.com/chriz-3656/Pulse-Music/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="150" height="150" alt="Pulse Music Logo">
  <h3>Music beyond silence.</h3>
  <p>A highly immersive, aesthetically driven Android music player featuring a custom <strong>Yellow Neumorphism</strong> and <strong>Skeuomorphic</strong> hardware design language.</p>
</div>

---

## 📥 Download Beta Release (v2.5.2)
Welcome to the experimental **Pulse JAM Beta**! This is a test version of the app featuring our brand-new Live Sync Rooms architecture. 

**How to Install:**
1. Navigate to the [Actions tab](actions) of this repository.
2. Click on the latest successful workflow run for the `feature/test-jam-sync` branch.
3. Scroll down to the **Artifacts** section at the bottom of the page.
4. Download the `pulse-music-apks.zip` file, extract it, and install the `app-debug.apk` directly on your Android device!

---

## ✨ Features

### 🎧 Pulse JAM (Live Sync Rooms)
- **Real-Time Synchronization:** Create and join live music rooms using a fully synchronized Firebase Realtime Queue!
- **Zero Drift:** ExoPlayer timestamp drift is dynamically monitored and auto-corrected (throtteled to prevent seek loops).
- **In-Room Search & Queueing:** Tap `+ ADD SONG` inside any Jam Room to dynamically search for tracks and broadcast them instantly to the global Room Queue.
- **Autoplay Bridging:** AI smart suggestions automatically forward into the Room Queue for everyone to hear when the queue ends.
- **Democratic Vote Skipping:** Guests can trigger `VOTE SKIP`, automatically advancing tracks once a 50% room consensus is reached.

### 🎨 Design & Experience
- **Neumorphic & Skeuomorphic UI:** Tactile buttons that physically sink into the screen, backlit LCD metadata screens, and glowing LED lamps.
- **Auto-Scrolling Time-Synced Lyrics:** Beautiful animated lyrics injected right into the main player UI, automatically syncing and scrolling with the track.
- **Live Hardware Vu Meter:** An analog needle meter that reacts in real-time to the music's audio session.
- **Poppins Typography:** Premium geometric sans-serif font *Poppins*, heavily enhancing readability.

### ⚙️ Core Audio Engine
- **Multi-Source Fetching:** Intelligent routing across YouTube Music (via NewPipe engine) and JioSaavn (320kbps streams).
- **Background Playback:** Media3 powered foreground service ensuring stable, uninterrupted playback.
- **Sleep Timer:** Securely handles pausing playback and shutting down the music service after a customizable period.
- **Offline Downloads:** Cache songs directly to your device storage for offline playback.

---

## 🏗️ Architecture
Pulse Music is built with modern Android development standards:
- **UI:** 100% Jetpack Compose (Kotlin DSL)
- **Audio:** Media3 ExoPlayer
- **Networking:** Retrofit2 & OkHttp
- **Database:** Room (Local cache) & Firebase Realtime Database (Pulse JAM)
- **Image Loading:** Coil

---

## 🚀 Building the App

This app relies on GitHub Actions for compilation and CI/CD. The local Gradle environment may time out on dependency resolution due to limited resources. Please push to the repository to trigger the `build.yml` GitHub Actions workflow.

If building locally:
```bash
./gradlew assembleDebug --no-daemon
```

---

## 📜 Versioning & History
- **Current Version:** v2.5.2
- **Latest Changes:** Improved Pulse JAM participant playback synchronization, drift correction smoothing, and race condition fixes. 

*See [CHANGELOG.md](CHANGELOG.md) for a complete history of updates.*

---

<div align="center">
  <p>Developed with ❤️ by <a href="https://github.com/chriz-3656">Chris Mon Saji</a></p>
</div>
