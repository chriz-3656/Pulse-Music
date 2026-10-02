import re

with open('app/src/main/java/com/example/data/repository/MusicRepositoryImpl.kt', 'r') as f:
    text = f.read()

old_code = """                val plain = json.optString("plainLyrics", "")
                if (plain.isNotBlank()) return plain
                val synced = json.optString("syncedLyrics", "")
                if (synced.isNotBlank()) {
                    return synced // Return raw LRC with timestamps!
                }"""

new_code = """                val synced = json.optString("syncedLyrics", "")
                if (synced.isNotBlank()) return synced // Return raw LRC with timestamps!
                val plain = json.optString("plainLyrics", "")
                if (plain.isNotBlank()) return plain"""

text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/data/repository/MusicRepositoryImpl.kt', 'w') as f:
    f.write(text)
