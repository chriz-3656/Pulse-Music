import re

with open('app/src/main/java/com/example/di/AppContainer.kt', 'r') as f:
    text = f.read()

old_code = """    val musicRepository: MusicRepository by lazy {
        MusicRepositoryImpl(context, musicDao, jioSaavnClient, ytmHelper)
    }"""
new_code = """    val jamSessionManager: com.example.data.repository.JamSessionManager by lazy {
        com.example.data.repository.JamSessionManager()
    }

    val musicRepository: MusicRepository by lazy {
        MusicRepositoryImpl(context, musicDao, jioSaavnClient, ytmHelper)
    }"""
text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/di/AppContainer.kt', 'w') as f:
    f.write(text)
