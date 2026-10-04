package com.example.data.repository

import com.example.domain.model.JamRoom
import com.example.domain.model.Song
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class JamSessionManager {
    private val database = FirebaseDatabase.getInstance("https://pulse-musicapp-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
    private val auth = FirebaseAuth.getInstance()

    var currentUserId: String = ""
        private set

    suspend fun authenticateAnonymously() {
        val user = auth.currentUser
        if (user == null) {
            val result = auth.signInAnonymously().await()
            currentUserId = result.user?.uid ?: ""
        } else {
            currentUserId = user.uid
        }
    }

    suspend fun createRoom(hostName: String): String {
        if (currentUserId.isEmpty()) authenticateAnonymously()
        
        // Generate a 6-digit random code
        val roomCode = (100000..999999).random().toString()
        val roomRef = database.child("rooms").child(roomCode)
        
        val newRoom = JamRoom(
            roomId = roomCode,
            hostId = currentUserId,
            participants = mapOf(currentUserId to com.example.domain.model.JamParticipant(name = hostName, isHost = true))
        )
        
        roomRef.setValue(newRoom).await()
        return roomCode
    }

    suspend fun joinRoom(roomCode: String, guestName: String) {
        if (currentUserId.isEmpty()) authenticateAnonymously()
        val participantRef = database.child("rooms").child(roomCode).child("participants").child(currentUserId)
        participantRef.setValue(com.example.domain.model.JamParticipant(name = guestName, isHost = false)).await()
    }

    suspend fun leaveRoom(roomCode: String) {
        if (currentUserId.isEmpty()) return
        database.child("rooms").child(roomCode).child("participants").child(currentUserId).removeValue().await()
    }

    suspend fun endRoom(roomCode: String) {
        if (currentUserId.isEmpty()) return
        database.child("rooms").child(roomCode).removeValue().await()
    }

    suspend fun kickParticipant(roomCode: String, participantId: String) {
        if (currentUserId.isEmpty()) return
        database.child("rooms").child(roomCode).child("participants").child(participantId).removeValue().await()
    }

    suspend fun removeFromQueue(roomCode: String, index: Int) {
        val queueRef = database.child("rooms").child(roomCode).child("queue")
        val snapshot = queueRef.get().await()
        val currentQueue = mutableListOf<com.example.domain.model.Song>()
        for (child in snapshot.children) {
            val s = child.getValue(com.example.domain.model.Song::class.java)
            if (s != null) currentQueue.add(s)
        }
        if (index in 0 until currentQueue.size) {
            currentQueue.removeAt(index)
            queueRef.setValue(currentQueue).await()
        }
    }

    suspend fun addToQueue(roomCode: String, song: com.example.domain.model.Song) {
        val queueRef = database.child("rooms").child(roomCode).child("queue")
        // Get current queue
        val snapshot = queueRef.get().await()
        val currentQueue = mutableListOf<com.example.domain.model.Song>()
        for (child in snapshot.children) {
            val s = child.getValue(com.example.domain.model.Song::class.java)
            if (s != null) currentQueue.add(s)
        }
        currentQueue.add(song)
        queueRef.setValue(currentQueue).await()
    }

    fun observeRoom(roomCode: String): Flow<JamRoom?> = callbackFlow {
        val ref = database.child("rooms").child(roomCode)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.getValue(JamRoom::class.java)
                trySend(room)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updatePlaybackState(roomCode: String, state: com.example.domain.model.JamPlaybackState) {
        database.child("rooms").child(roomCode).child("playback").setValue(state).await()
    }

    suspend fun voteSkip(roomCode: String) {
        if (currentUserId.isEmpty()) return
        val ref = database.child("rooms").child(roomCode).child("skipVotes")
        val snapshot = ref.get().await()
        
        val currentVotes = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
        if (!currentVotes.contains(currentUserId)) {
            currentVotes.add(currentUserId)
            ref.setValue(currentVotes).await()
        }
    }

    suspend fun resetSkipVotes(roomCode: String) {
        database.child("rooms").child(roomCode).child("skipVotes").removeValue().await()
    }
}
