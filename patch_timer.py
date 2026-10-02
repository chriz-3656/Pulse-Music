import re

with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'r') as f:
    text = f.read()

# Add mutableStateOf for dialog
if 'var showTimerDialog by remember { mutableStateOf(false) }' not in text:
    text = text.replace('val uiState by viewModel.uiState.collectAsState()', 'val uiState by viewModel.uiState.collectAsState()\n    var showTimerDialog by remember { mutableStateOf(false) }')

# Add Timer button between Lyrics and Queue
old_buttons = """                SkeuoTactileButton(
                    onClick = { viewModel.toggleQueue() },
                    isPressedOrActive = uiState.showQueue,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(32.dp).testTag("player_queue_button")
                ) {"""

new_buttons = """                SkeuoTactileButton(
                    onClick = { showTimerDialog = true },
                    isPressedOrActive = playerState.sleepTimerTimeLeftMs != null,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(32.dp).testTag("player_timer_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val timerText = if (playerState.sleepTimerTimeLeftMs != null) {
                            val min = playerState.sleepTimerTimeLeftMs!! / 60000
                            val sec = (playerState.sleepTimerTimeLeftMs!! % 60000) / 1000
                            String.format("%d:%02d", min, sec)
                        } else "TIMER"
                        Icon(androidx.compose.material.icons.Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(timerText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp), color = MaterialTheme.colorScheme.onBackground)
                    }
                }

                SkeuoTactileButton(
                    onClick = { viewModel.toggleQueue() },
                    isPressedOrActive = uiState.showQueue,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(32.dp).testTag("player_queue_button")
                ) {"""
text = text.replace(old_buttons, new_buttons)

# Add Dialog at the end of the composable
dialog_code = """    // Queue Bottom Sheet"""
new_dialog_code = """    if (showTimerDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                Column {
                    val options = listOf(0, 15, 30, 60, 120)
                    options.forEach { mins ->
                        androidx.compose.material3.TextButton(
                            onClick = { 
                                viewModel.setSleepTimer(mins)
                                showTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (mins == 0) "Turn Off" else "$mins Minutes")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTimerDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    // Queue Bottom Sheet"""
text = text.replace(dialog_code, new_dialog_code)

# Fix Arrangement of the button row
text = text.replace('horizontalArrangement = Arrangement.SpaceBetween,\n                verticalAlignment = Alignment.CenterVertically', 'horizontalArrangement = Arrangement.SpaceEvenly,\n                verticalAlignment = Alignment.CenterVertically')

with open('app/src/main/java/com/example/ui/screens/FullScreenPlayerScreen.kt', 'w') as f:
    f.write(text)
