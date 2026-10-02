# Changelog

## [2.1.3] - 2026-10-02
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

