package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.JamUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamLobbyDialog(
    uiState: JamUiState,
    onDismiss: () -> Unit,
    onCreateRoom: (String) -> Unit,
    onJoinRoom: (String, String) -> Unit
) {
    var hostName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var joinName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "PULSE JAM",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Listen together in real-time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isConnecting) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(24.dp))
            } else if (uiState.errorMessage != null) {
                Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
            } else if (uiState.activeRoom != null) {
                val room = uiState.activeRoom
                SkeuoBevelCard(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ROOM CODE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(room.roomId, style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 8.sp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("${room.participants.size} Participant(s)", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("GO TO PLAYER", fontWeight = FontWeight.Bold)
                }
                return@Column
            }

            // Create Room Section
            OutlinedTextField(
                value = hostName,
                onValueChange = { hostName = it },
                label = { Text("Your Nickname") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onCreateRoom(hostName) },
                enabled = hostName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CREATE ROOM", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("OR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            // Join Room Section
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = joinCode,
                    onValueChange = { joinCode = it },
                    label = { Text("Room Code") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = joinName,
                    onValueChange = { joinName = it },
                    label = { Text("Nickname") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onJoinRoom(joinCode, joinName) },
                enabled = joinCode.isNotBlank() && joinName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("JOIN ROOM", fontWeight = FontWeight.Bold)
            }
        }
    }
}
