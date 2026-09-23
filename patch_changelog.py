with open("CHANGELOG.md", "r") as f:
    text = f.read()

new_log = """## [2.1.0] - 2026-09-23

### Added
- **Spotify Playlist Import**: Paste a public Spotify Playlist URL to automatically scrape tracks, match them to YouTube Music streams, and build a native offline Pulse Music crate.
- **Smart Rate Limiting**: Intelligent backend throttling to seamlessly handle massive playlist scraping without hitting provider API limits.
- **Local Playlist Architecture**: Playlists now fully resolve from the local SQLite database for lightning-fast loading offline.

---

## [2.0.0] - """

text = text.replace("## [2.0.0] - ", new_log)

with open("CHANGELOG.md", "w") as f:
    f.write(text)
