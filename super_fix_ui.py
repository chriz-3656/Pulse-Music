import re

with open("app/src/main/java/com/example/ui/screens/LibraryScreen.kt", "r") as f:
    text = f.read()

# Fix literal \n in SkeuoAmberGlow
text = text.replace("import com.example.ui.theme.SkeuoAmberGlow\\nimport com.example.ui.theme.SkeuoPeakRed", "import com.example.ui.theme.SkeuoAmberGlow\\nimport com.example.ui.theme.SkeuoPeakRed") # Wait, in python `\\n` is backslash n. I need to do string replacement!
