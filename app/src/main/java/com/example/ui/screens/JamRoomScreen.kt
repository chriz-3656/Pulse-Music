package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SkeuoBevelCard
import com.example.ui.viewmodel.JamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamRoomScreen(
    jamViewModel: JamViewModel,
    onBackClick: () -> Unit,
    onGoToPlayer: () -> Unit
) {
    val uiState by jamViewModel.uiState.collectAsState()
    val activeRoom = uiState.activeRoom

    if (activeRoom == null) {
        // Room ended or disconnected
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("No active Jam Room", color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBackClick) { Text("Go Back") }
        }
        return
    }

    val isHost = activeRoom.hostId == jamViewModel.jamSessionManager.currentUserId

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("LIVE JAM ROOM", fontWeight = FontWeight.Black) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.primary
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SkeuoBevelCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ROOM CODE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeRoom.roomId,
                            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 12.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onGoToPlayer,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("OPEN AUDIO PLAYER", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            item {
                Text("PARTICIPANTS (${activeRoom.participants.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(activeRoom.participants.values.toList()) { participant ->
                SkeuoBevelCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), isRecessed = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (participant.isHost) Icons.Default.Star else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (participant.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = participant.name,
                            fontWeight = if (participant.isHost) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (participant.isHost) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text("HOST", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                if (isHost) {
                    Button(
                        onClick = { jamViewModel.endRoom(); onBackClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("END JAM SESSION", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                    }
                } else {
                    Button(
                        onClick = { jamViewModel.leaveRoom(); onBackClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("LEAVE JAM SESSION", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }
}
