import re

with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'r') as f:
    text = f.read()

# Fix mutableStateOf
if 'var showTimerDialog by remember { mutableStateOf(false) }' not in text:
    text = text.replace('val uiState by viewModel.uiState.collectAsStateWithLifecycle()', 'val uiState by viewModel.uiState.collectAsStateWithLifecycle()\n    var showTimerDialog by remember { mutableStateOf(false) }')

# Fix Icon
text = text.replace('androidx.compose.material.icons.Icons.Default.Timer', 'androidx.compose.material.icons.filled.Timer')

# Add missing Timer icon import
if 'import androidx.compose.material.icons.filled.Timer' not in text:
    text = text.replace('import androidx.compose.material.icons.filled.SkipPrevious', 'import androidx.compose.material.icons.filled.SkipPrevious\nimport androidx.compose.material.icons.filled.Timer')

with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'w') as f:
    f.write(text)
