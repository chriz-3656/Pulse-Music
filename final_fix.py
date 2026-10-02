import re
import os

# Fix FullScreenPlayerScreen
with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('import com.example.player.RepeatMode', 'import com.example.domain.model.RepeatMode')
text = text.replace('song.albumArtUrl', 'song.artworkUrl')
text = text.replace('playerState.duration', 'playerState.totalDurationMs')
text = text.replace('playerState.currentPosition', 'playerState.currentPositionMs')
text = text.replace('viewModel.playPrevious()', 'viewModel.skipToPrevious()')
text = text.replace('viewModel.playNext()', 'viewModel.skipToNext()')
text = text.replace('letterSpacing = 2.sp', 'letterSpacing = 2.sp')
if 'import androidx.compose.ui.unit.sp' not in text:
    text = text.replace('import androidx.compose.ui.unit.dp', 'import androidx.compose.ui.unit.dp\nimport androidx.compose.ui.unit.sp')

with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'w') as f:
    f.write(text)

# Fix MiniPlayerBar
with open('app/src/main/java/com/example/ui/components/MiniPlayerBar.kt', 'r') as f:
    text = f.read()

if 'import com.example.domain.model.PlayerState' not in text:
    text = text.replace('import com.example.player.PlayerState', 'import com.example.domain.model.PlayerState')
text = text.replace('song.albumArtUrl', 'song.artworkUrl')
text = text.replace('playerState.duration', 'playerState.totalDurationMs')
text = text.replace('playerState.currentPosition', 'playerState.currentPositionMs')

with open('app/src/main/java/com/example/ui/components/MiniPlayerBar.kt', 'w') as f:
    f.write(text)

# Fix HomeScreen
with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    text = f.read()

if 'import androidx.compose.ui.draw.shadow' not in text:
    text = text.replace('import androidx.compose.ui.draw.clip', 'import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.shadow')
text = text.replace('album.albumArtUrl', 'album.artworkUrl')
text = text.replace('playlist.coverUrl', 'playlist.artworkUrl')
text = text.replace('playlist.songIds.size', 'playlist.trackCount')

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(text)

# Fix AlbumDetailScreen
with open('app/src/main/java/com/example/ui/screens/AlbumDetailScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('SkeuoKnobGrip', 'MaterialTheme.colorScheme.surfaceVariant')
with open('app/src/main/java/com/example/ui/screens/AlbumDetailScreen.kt', 'w') as f:
    f.write(text)

