with open("app/src/main/java/com/example/data/repository/MusicRepositoryImpl.kt", "r") as f:
    lines = f.readlines()

with open("app/src/main/java/com/example/data/repository/MusicRepositoryImpl.kt", "w") as f:
    for line in lines:
        if 'val jsonMatch = Regex' in line:
            f.write('            val jsonMatch = Regex("<script id=\\"__NEXT_DATA__\\" type=\\"application/json\\">(.*?)</script>").find(html)\\n')
        else:
            f.write(line)
