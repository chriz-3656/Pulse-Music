with open("app/src/main/java/com/example/ui/screens/LibraryScreen.kt", "r") as f:
    text = f.read()

# Fix the duplicate in declaration
bad_decl = """fun PlaylistsTabContent(
                        onImportSpotifyClick = { showSpotifyDialog = true },
    onImportSpotifyClick: () -> Unit,"""
good_decl = """fun PlaylistsTabContent(
    onImportSpotifyClick: () -> Unit,"""
text = text.replace(bad_decl, good_decl)

# Check if import is missing
if "import com.example.ui.theme.SkeuoPeakRed" not in text:
    text = text.replace("import com.example.ui.theme.SkeuoAmberGlow", "import com.example.ui.theme.SkeuoAmberGlow\\nimport com.example.ui.theme.SkeuoPeakRed")

with open("app/src/main/java/com/example/ui/screens/LibraryScreen.kt", "w") as f:
    f.write(text)
