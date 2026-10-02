import re

with open('app/src/main/java/com/example/ui/viewmodel/MusicViewModels.kt', 'r') as f:
    text = f.read()

old_code = """    fun toggleLyrics() {
        _uiState.update { it.copy(showLyricsSheet = !it.showLyricsSheet) }
    }
}"""

new_code = """    fun toggleLyrics() {
        _uiState.update { it.copy(showLyricsSheet = !it.showLyricsSheet) }
    }

    fun setSleepTimer(minutes: Int) {
        playerController.setSleepTimer(minutes)
    }
}"""

text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/ui/viewmodel/MusicViewModels.kt', 'w') as f:
    f.write(text)
