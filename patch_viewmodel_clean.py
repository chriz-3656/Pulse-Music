import re

with open('app/src/main/java/com/example/ui/viewmodel/MusicViewModels.kt', 'r') as f:
    text = f.read()

old_code = """    fun setSleepTimer(minutes: Int) {
        playerController.setSleepTimer(minutes)
    }

    fun toggleAutoplay() {"""

new_code = """    fun toggleAutoplay() {"""

text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/ui/viewmodel/MusicViewModels.kt', 'w') as f:
    f.write(text)
