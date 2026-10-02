import re

with open('app/src/main/java/com/example/domain/model/PlayerState.kt', 'r') as f:
    text = f.read()

# Add sleepTimerTimeLeftMs to PlayerState data class
if 'val sleepTimerTimeLeftMs: Long? = null' not in text:
    old_code = "val isAudioFocusLoss: Boolean = false"
    new_code = "val isAudioFocusLoss: Boolean = false,\n    val sleepTimerTimeLeftMs: Long? = null"
    text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/domain/model/PlayerState.kt', 'w') as f:
    f.write(text)
