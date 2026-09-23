import re

with open("app/src/main/java/com/example/ui/screens/LibraryScreen.kt", "r") as f:
    text = f.read()

# Add UI state for Spotify Dialog
if "var showSpotifyDialog by remember { mutableStateOf(false) }" not in text:
    state_vars = """    var newPlaylistTitle by remember { mutableStateOf("") }
    var showSpotifyDialog by remember { mutableStateOf(false) }
    var spotifyUrl by remember { mutableStateOf("") }"""
    text = text.replace('    var newPlaylistTitle by remember { mutableStateOf("") }', state_vars)

# Add Import Button
import_card = """        // Import Spotify Card
        item {
            Spacer(modifier = Modifier.height(12.dp))
            SkeuoBevelCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSpotifyDialog = true },
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SkeuoRecessedTray)
                            .border(BorderStroke(1.dp, SkeuoChromeDark), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddLink,
                            contentDescription = "Import Spotify",
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Import from Spotify",
                            style = MaterialTheme.typography.titleMedium,
                            color = SkeuoTextPrimary
                        )
                        Text(
                            "Sync external playlists",
                            style = MaterialTheme.typography.bodySmall,
                            color = SkeuoTextSecondary
                        )
                    }
                }
            }
        }

        items(playlists) {"""
text = text.replace("        items(playlists) {", import_card)

# Add Spotify Dialogs
dialogs = """        // Spotify Import Dialog
        if (showSpotifyDialog) {
            AlertDialog(
                onDismissRequest = { showSpotifyDialog = false },
                containerColor = SkeuoDeckElevated,
                title = {
                    Text(
                        "IMPORT SPOTIFY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF1DB954)
                    )
                },
                text = {
                    Column {
                        Text("Paste a public Spotify playlist link to import its tracks directly into Pulse Music.", color = SkeuoTextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = spotifyUrl,
                            onValueChange = { spotifyUrl = it },
                            label = { Text("Spotify URL", color = SkeuoTextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1DB954),
                                focusedLabelColor = Color(0xFF1DB954),
                                unfocusedBorderColor = SkeuoChromeDark,
                                unfocusedLabelColor = SkeuoTextSecondary,
                                cursorColor = Color(0xFF1DB954),
                                focusedTextColor = SkeuoTextPrimary,
                                unfocusedTextColor = SkeuoTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (spotifyUrl.isNotBlank()) {
                                viewModel.importSpotifyPlaylist(spotifyUrl)
                                showSpotifyDialog = false
                                spotifyUrl = ""
                            }
                        }
                    ) {
                        Text("IMPORT", color = Color(0xFF1DB954))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSpotifyDialog = false }) {
                        Text("CANCEL", color = SkeuoTextSecondary)
                    }
                }
            )
        }
        
        // Progress Dialog
        uiState.importProgress?.let { progress ->
            AlertDialog(
                onDismissRequest = { if (progress.isComplete) viewModel.dismissImportDialog() },
                containerColor = SkeuoDeckElevated,
                title = { Text(if (progress.isComplete) "IMPORT COMPLETE" else "IMPORTING...", color = SkeuoTextPrimary) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(progress.message, color = SkeuoTextSecondary, modifier = Modifier.padding(bottom = 16.dp))
                        if (!progress.isComplete) {
                            LinearProgressIndicator(
                                progress = { progress.progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF1DB954),
                                trackColor = SkeuoRecessedTray
                            )
                        } else if (progress.error != null) {
                            Text(progress.error, color = SkeuoPeakRed)
                        }
                    }
                },
                confirmButton = {
                    if (progress.isComplete) {
                        TextButton(onClick = { viewModel.dismissImportDialog() }) {
                            Text("OK", color = SkeuoAmberGlow)
                        }
                    }
                }
            )
        }

        // Create Playlist Dialog"""
text = text.replace("        // Create Playlist Dialog", dialogs)

with open("app/src/main/java/com/example/ui/screens/LibraryScreen.kt", "w") as f:
    f.write(text)
