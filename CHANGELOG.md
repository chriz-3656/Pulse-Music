# Changelog

## [2.5.0] - 2026-10-04
### Added
- **Pulse JAM (Live Sync Rooms):** Create and join live music rooms! Features a fully synchronized Firebase Realtime Queue where everyone in the room listens to the exact same music in perfect sync.
- **In-Room Search & Queueing:** Tap '+ ADD SONG' inside any Jam Room to dynamically search for tracks and instantly broadcast them to the global Room Queue.
- **Autoplay Suggestion Bridging:** When the Room Host reaches the end of the queue, the Music Engine automatically generates smart suggestions and seamlessly pushes them directly into the Firebase Room Queue for everyone to enjoy.
- **Host Room Controls:** Hosts can kick participants and effortlessly control the playback state for all listeners.
- **One-Tap Room Invites:** Simply tap the massive 6-digit Room Code at the top of the Jam Room to copy it to your clipboard.

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

