import re

with open('app/src/main/java/com/example/player/MusicPlayerController.kt', 'r') as f:
    text = f.read()

old_code = """            _playerState.update { it.copy(sleepTimerTimeLeftMs = 0L) }
            pause()
            
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = MusicPlaybackService.ACTION_STOP
            }"""

new_code = """            _playerState.update { it.copy(sleepTimerTimeLeftMs = 0L, isPlaying = false) }
            onServiceCommand?.invoke(ServiceAction.Pause)
            
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = MusicPlaybackService.ACTION_STOP
            }"""

text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/player/MusicPlayerController.kt', 'w') as f:
    f.write(text)
