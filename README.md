# Pulse Music v2.1.3

Pulse Music is a highly immersive, aesthetically driven Android music player featuring a custom **Yellow Neumorphism** and **Skeuomorphic** hardware design language. 

## Features
- **Neumorphic & Skeuomorphic UI:** Tactile buttons that sink into the screen, backlit LCD metadata screens, and glowing LED lamps.
- **Auto-Scrolling Time-Synced Lyrics:** Beautiful animated lyrics injected right into the main player UI, automatically syncing and scrolling with the track.
- **Sleep Timer:** Securely handles pausing playback and shutting down the music service after a customizable period.
- **Poppins Typography:** Premium geometric sans-serif font *Poppins*, enhancing readability.
- **Live Hardware Vu Meter:** Analog needle meter that reacts in real-time to the music's audio session.
- **Offline Downloads:** Cache songs directly to your device storage.

## Building the App
This app relies on GitHub Actions for compilation and CI/CD. The local Gradle environment may time out on dependency resolution due to limited resources. Please push to the repository to trigger the `build.yml` GitHub Actions workflow.

## Versioning
- **Current Version:** v2.1.3
- **Latest Changes:** Added time-synced lyrics, sleep timer, Poppins font, removed buggy Spotify integrations, and improved Neumorphic margins. See `CHANGELOG.md` for full details.
