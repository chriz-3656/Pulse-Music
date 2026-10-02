import re

with open('app/src/main/java/com/example/ui/PulseMusicApp.kt', 'r') as f:
    text = f.read()

text = text.replace('onBarClick = { isFullScreenPlayerOpen = true }', 'onNavigateToPlayer = { isFullScreenPlayerOpen = true }')
text = re.sub(r'FullScreenPlayerScreen\(\s*viewModel = playerViewModel,\s*onDismiss = \{ isFullScreenPlayerOpen = false \},\s*onArtistClick = \{ artistId ->\s*subScreenBackStack\.add\(SubScreen\.Artist\(artistId\)\)\s*\},\s*onAlbumClick = \{ albumId ->\s*subScreenBackStack\.add\(SubScreen\.Album\(albumId\)\)\s*\}\s*\)', 'FullScreenPlayerScreen(viewModel = playerViewModel, onBackClick = { isFullScreenPlayerOpen = false })', text)

with open('app/src/main/java/com/example/ui/PulseMusicApp.kt', 'w') as f:
    f.write(text)
