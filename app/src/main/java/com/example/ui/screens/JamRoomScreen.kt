package com.example.ui.screens

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.SkeuoBevelCard
import com.example.ui.components.SkeuoTactileButton
import com.example.ui.viewmodel.JamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamRoomScreen(
    jamViewModel: JamViewModel,
    onBackClick: () -> Unit,
    onGoToPlayer: () -> Unit,
    onPlaySong: (com.example.domain.model.Song) -> Unit
) {
    val uiState by jamViewModel.uiState.collectAsState()
    val activeRoom = uiState.activeRoom
    val searchResults by jamViewModel.searchResults.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    if (activeRoom == null) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("No active Jam Room", color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(24.dp))
            SkeuoTactileButton(onClick = onBackClick, modifier = Modifier.height(50.dp).width(150.dp)) {
                Text("Go Back", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    val isHost = activeRoom.hostId == jamViewModel.jamSessionManager.currentUserId

    Column(modifier = Modifier.fillMaxSize()) {
        // Neumorphic Custom Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp).systemBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeuoTactileButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("LIVE JAM ROOM", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
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
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(activeRoom.roomId))
                                Toast.makeText(context, "Room Code Copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        SkeuoTactileButton(
                            onClick = onGoToPlayer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            accentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text("OPEN AUDIO PLAYER", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("PARTICIPANTS (${activeRoom.participants.size})", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items(activeRoom.participants.entries.toList()) { (participantId, participant) ->
                SkeuoBevelCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), isRecessed = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                        Spacer(modifier = Modifier.weight(1f))
                        if (participant.isHost) {
                            Text("HOST", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                        } else if (isHost) {
                            SkeuoTactileButton(
                                onClick = { jamViewModel.kickParticipant(participantId) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Kick", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item {
                val isHost = activeRoom.hostId == jamViewModel.jamSessionManager.currentUserId
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("UPCOMING QUEUE", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (!isSearchOpen && activeRoom.queue.isNotEmpty()) {
                        SkeuoTactileButton(
                            onClick = { if (isHost) jamViewModel.forceSkip() else jamViewModel.voteSkip() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(36.dp).padding(end = 8.dp),
                            accentColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Text(if (isHost) "SKIP" else "VOTE SKIP", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                        }
                    }

                    SkeuoTactileButton(
                        onClick = { isSearchOpen = !isSearchOpen },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(if (isSearchOpen) "CLOSE SEARCH" else "+ ADD SONG", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }

            if (isSearchOpen) {
                item {
                    SkeuoBevelCard(modifier = Modifier.fillMaxWidth(), isRecessed = true) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it; jamViewModel.searchSongs(it) },
                            placeholder = { Text("Search to add to queue...") },
                            leadingIcon = { Icon(Icons.Outlined.Search, null) },
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                items(searchResults) { song ->
                    SkeuoTactileButton(
                        onClick = { 
                            jamViewModel.addToQueue(song)
                            searchQuery = ""
                            jamViewModel.searchSongs("")
                            isSearchOpen = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(64.dp).padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = song.artworkUrl,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (activeRoom.queue.isEmpty() && !isSearchOpen) {
                item {
                    Text("No upcoming songs.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                }
            }

            if (!isSearchOpen) {
                val isHost = activeRoom.hostId == jamViewModel.jamSessionManager.currentUserId

                itemsIndexed(activeRoom.queue) { index, song ->
                    SkeuoTactileButton(
                        onClick = { onPlaySong(song) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(64.dp).padding(vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = song.artworkUrl,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (isHost) {
                                IconButton(onClick = { jamViewModel.removeFromQueue(index) }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                if (isHost) {
                    SkeuoTactileButton(
                        onClick = { jamViewModel.endRoom(); onBackClick() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        accentColor = MaterialTheme.colorScheme.error
                    ) {
                        Text("END JAM SESSION", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    SkeuoTactileButton(
                        onClick = { jamViewModel.leaveRoom(); onBackClick() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        accentColor = MaterialTheme.colorScheme.error
                    ) {
                        Text("LEAVE JAM SESSION", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(48.dp)) // padding at bottom
            }
        }
    }
}
