import re

with open("app/src/main/java/com/example/domain/model/MusicModels.kt", "r") as f:
    text = f.read()

import_progress = """data class ImportProgress(
    val progress: Float,
    val message: String,
    val isComplete: Boolean = false,
    val playlistId: String? = null,
    val error: String? = null
)

data class SearchResults("""

text = text.replace("data class SearchResults(", import_progress)

with open("app/src/main/java/com/example/domain/model/MusicModels.kt", "w") as f:
    f.write(text)
