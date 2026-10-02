import re

with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'r') as f:
    text = f.read()

# Fix lyrics source to use uiState.lyrics and remove .weight(1f) to use fixed height
old_code = """            // Synced Lyrics View (Animated)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                SyncedLyricsView(
                    lyrics = playerState.currentSong?.lyrics,
                    currentPositionMs = playerState.currentPositionMs,
                    modifier = Modifier.fillMaxSize()
                )
            }"""

new_code = """            // Synced Lyrics View (Animated)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                SyncedLyricsView(
                    lyrics = uiState.lyrics ?: playerState.currentSong?.lyrics,
                    currentPositionMs = playerState.currentPositionMs,
                    modifier = Modifier.fillMaxSize()
                )
            }"""

text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'w') as f:
    f.write(text)
