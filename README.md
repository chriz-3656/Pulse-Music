# Pulse Music v2.5.1

Pulse Music is a highly immersive, aesthetically driven Android music player featuring a custom **Yellow Neumorphism** and **Skeuomorphic** hardware design language. 

## 📥 Download Beta Release (v2.5.1)
Welcome to the experimental **Pulse JAM Beta**! This is a test version of the app featuring our brand-new Live Sync Rooms architecture. 

**How to Install:**
1. Navigate to the [Actions tab](actions) of this repository.
2. Click on the latest successful workflow run for the `feature/pulse-jam` branch.
3. Scroll down to the **Artifacts** section at the bottom of the page.
4. Download the `pulse-music-apks.zip` file, extract it, and install the `app-debug.apk` directly on your Android device!

## Features
- **Pulse JAM (Live Sync Rooms):** Create and join live music rooms with a fully synchronized Firebase Realtime Queue!
- **Autoplay Suggestion Bridging:** AI smart suggestions automatically forward into the Room Queue for everyone to hear.
- **Neumorphic & Skeuomorphic UI:** Tactile buttons that sink into the screen, backlit LCD metadata screens, and glowing LED lamps.
- **Auto-Scrolling Time-Synced Lyrics:** Beautiful animated lyrics injected right into the main player UI, automatically syncing and scrolling with the track.
- **Sleep Timer:** Securely handles pausing playback and shutting down the music service after a customizable period.
- **Poppins Typography:** Premium geometric sans-serif font *Poppins*, enhancing readability.
- **Live Hardware Vu Meter:** Analog needle meter that reacts in real-time to the music's audio session.
- **Offline Downloads:** Cache songs directly to your device storage.

## Building the App
This app relies on GitHub Actions for compilation and CI/CD. The local Gradle environment may time out on dependency resolution due to limited resources. Please push to the repository to trigger the `build.yml` GitHub Actions workflow.

## Versioning
- **Current Version:** v2.5.1
- **Latest Changes:** Introduced Pulse JAM (Live Sync Rooms), In-Room Search & Queueing, Autoplay Suggestion Bridging, and massive scrolling optimizations. See `CHANGELOG.md` for full details.
