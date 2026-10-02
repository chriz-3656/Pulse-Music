import re

with open('app/src/main/java/com/example/player/MusicPlayerController.kt', 'r') as f:
    text = f.read()

# Add sleepTimer functions
if 'fun setSleepTimer' not in text:
    old_code = "val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()"
    new_code = """val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var sleepTimerJob: kotlinx.coroutines.Job? = null

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playerState.update { it.copy(sleepTimerTimeLeftMs = null) }
            return
        }

        sleepTimerJob = scope.launch {
            var timeLeftMs = minutes * 60 * 1000L
            while (timeLeftMs > 0) {
                _playerState.update { it.copy(sleepTimerTimeLeftMs = timeLeftMs) }
                kotlinx.coroutines.delay(1000L)
                timeLeftMs -= 1000L
            }
            _playerState.update { it.copy(sleepTimerTimeLeftMs = 0L) }
            pause()
            
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = MusicPlaybackService.ACTION_STOP
            }
            context.startService(intent)
            
            _playerState.update { it.copy(sleepTimerTimeLeftMs = null) }
        }
    }"""
    text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/player/MusicPlayerController.kt', 'w') as f:
    f.write(text)
