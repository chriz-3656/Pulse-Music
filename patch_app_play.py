import re

with open('app/src/main/java/com/example/ui/PulseMusicApp.kt', 'r') as f:
    text = f.read()

target = '''                        is SubScreen.JamRoom -> {
                            JamRoomScreen(
                                jamViewModel = jamViewModel,
                                onBackClick = { subScreenBackStack.removeAt(subScreenBackStack.size - 1) },
                                onGoToPlayer = { isFullScreenPlayerOpen = true }
                            )
                        }'''
replacement = '''                        is SubScreen.JamRoom -> {
                            JamRoomScreen(
                                jamViewModel = jamViewModel,
                                onBackClick = { subScreenBackStack.removeAt(subScreenBackStack.size - 1) },
                                onGoToPlayer = { isFullScreenPlayerOpen = true },
                                onPlaySong = { song -> homeViewModel.playSong(song) }
                            )
                        }'''
text = text.replace(target, replacement)

with open('app/src/main/java/com/example/ui/PulseMusicApp.kt', 'w') as f:
    f.write(text)
