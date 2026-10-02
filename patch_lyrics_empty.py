import re

with open('app/src/main/java/com/example/ui/components/SyncedLyricsView.kt', 'r') as f:
    text = f.read()

# Replace the NO LYRICS box with nothing (just return)
old_code_1 = """    if (lyrics.isNullOrBlank()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "NO LYRICS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = LocalNeuColors.current.textSecondary.copy(alpha = 0.3f)
                )
            )
        }
        return
    }"""

new_code_1 = """    if (lyrics.isNullOrBlank()) {
        return
    }"""

old_code_2 = """    if (parsedLyrics.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = "LYRICS NOT SYNCED",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = LocalNeuColors.current.textSecondary.copy(alpha = 0.3f)
                )
            )
        }
        return
    }"""

new_code_2 = """    if (parsedLyrics.isEmpty()) {
        return
    }"""

text = text.replace(old_code_1, new_code_1)
text = text.replace(old_code_2, new_code_2)

with open('app/src/main/java/com/example/ui/components/SyncedLyricsView.kt', 'w') as f:
    f.write(text)
