import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    text = f.read()

# Add import for Jam UI
text = text.replace('import androidx.compose.ui.unit.sp', 'import androidx.compose.ui.unit.sp\nimport com.example.ui.components.JamLobbyDialog\nimport com.example.ui.viewmodel.JamViewModel\nimport androidx.compose.runtime.collectAsState\nimport androidx.compose.runtime.getValue')

# Add Jam UI state
old_fun = """fun HomeScreen(
    viewModel: HomeViewModel,"""
new_fun = """fun HomeScreen(
    viewModel: HomeViewModel,
    jamViewModel: JamViewModel,"""
text = text.replace(old_fun, new_fun)

old_state = """    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value"""
new_state = """    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val jamUiState by jamViewModel.uiState.collectAsState()
    var showJamDialog by remember { mutableStateOf(false) }"""
text = text.replace(old_state, new_state)

# Add JAM button next to settings
old_settings = """            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
            }"""
new_settings = """            IconButton(onClick = { showJamDialog = true }) {
                Icon(androidx.compose.material.icons.filled.Group, contentDescription = "Pulse Jam", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
            }"""
text = text.replace(old_settings, new_settings)

# Inject dialog at bottom
old_dialog = """    if (showImportDialog) {"""
new_dialog = """    if (showJamDialog) {
        JamLobbyDialog(
            uiState = jamUiState,
            onDismiss = { showJamDialog = false },
            onCreateRoom = { jamViewModel.createRoom(it) },
            onJoinRoom = { code, name -> jamViewModel.joinRoom(code, name) }
        )
    }

    if (showImportDialog) {"""
text = text.replace(old_dialog, new_dialog)

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(text)
