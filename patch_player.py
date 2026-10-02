import re

with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'r') as f:
    text = f.read()

if 'import com.example.ui.components.SyncedLyricsView' not in text:
    text = text.replace('import com.example.ui.components.SkeuoBevelCard', 'import com.example.ui.components.SyncedLyricsView\nimport com.example.ui.components.SkeuoBevelCard')

old_code = """            Spacer(modifier = Modifier.height(24.dp))

            // Backlit LCD Display Bay (Metadata & Status)"""

new_code = """            Spacer(modifier = Modifier.height(8.dp))
            
            // Synced Lyrics View (Animated)
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
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Backlit LCD Display Bay (Metadata & Status)"""

text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'w') as f:
    f.write(text)
