package com.example.data.repository

import android.content.Context
import com.example.domain.model.JamParticipant
import com.example.domain.model.JamPlaybackState
import com.example.domain.model.JamRoom
import com.example.domain.model.Song
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

class JamSessionManager(private val context: Context) {
    private val database by lazy {
        FirebaseDatabase.getInstance("https://pulse-musicapp-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
    }
    private val auth by lazy {
        FirebaseAuth.getInstance()
    }
    private val prefs = context.getSharedPreferences("pulse_music_settings", Context.MODE_PRIVATE)

    var currentUserId: String = ""
        private set

    private fun requireOnline() {
        check(!prefs.getBoolean("is_pure_offline", false)) {
            "Pulse JAM is unavailable in Pure Offline Mode."
        }
    }

    suspend fun authenticateAnonymously() {
        requireOnline()
        val user = auth.currentUser
        if (user == null) {
            val result = auth.signInAnonymously().await()
            currentUserId = result.user?.uid.orEmpty()
        } else {
            currentUserId = user.uid
        }
        check(currentUserId.isNotBlank()) { "Could not authenticate with Pulse JAM." }
    }

    suspend fun createRoom(hostName: String): String {
        requireAuthenticated()

        repeat(6) {
            val roomCode = (100000..999999).random().toString()
            val roomRef = database.child("rooms").child(roomCode)
            val newRoom = JamRoom(
                roomId = roomCode,
                hostId = currentUserId,
                participants = mapOf(
                    currentUserId to JamParticipant(name = hostName, isHost = true)
                )
            )

            // Claim the room atomically so two hosts cannot overwrite each other
            // when they pick the same random code at the same time.
            val committed = roomRef.runTransactionAwait { currentData ->
                if (currentData.value != null) {
                    Transaction.abort()
                } else {
                    currentData.value = newRoom
                    Transaction.success(currentData)
                }
            }

            if (committed) {
                roomRef.child("participants").child(currentUserId).onDisconnect().removeValue()
                return roomCode
            }
        }
        error("Could not create a unique JAM room. Please try again.")
    }

    suspend fun joinRoom(roomCode: String, guestName: String) {
        requireAuthenticated()
        val roomRef = database.child("rooms").child(roomCode)
        val snapshot = roomRef.get().await()
        check(snapshot.exists()) { "JAM room $roomCode was not found." }

        val participants = snapshot.child("participants")
        val alreadyJoined = participants.child(currentUserId).exists()
        check(alreadyJoined || participants.childrenCount < MAX_PARTICIPANTS) {
            "This JAM room is full."
        }

        val participantRef = roomRef.child("participants").child(currentUserId)
        participantRef.setValue(JamParticipant(name = guestName, isHost = false)).await()
        participantRef.onDisconnect().removeValue()
    }

    suspend fun leaveRoom(roomCode: String) {
        if (currentUserId.isBlank()) return
        database.child("rooms").child(roomCode).child("participants")
            .child(currentUserId)
            .removeValue()
            .await()
    }

    suspend fun endRoom(roomCode: String) {
        requireHost(roomCode)
        database.child("rooms").child(roomCode).removeValue().await()
    }

    suspend fun kickParticipant(roomCode: String, participantId: String) {
        requireHost(roomCode)
        check(participantId != currentUserId) { "The host cannot remove themselves." }
        database.child("rooms").child(roomCode).child("participants")
            .child(participantId)
            .removeValue()
            .await()
    }

    suspend fun removeFromQueue(roomCode: String, index: Int) {
        requireHost(roomCode)
        val queueRef = database.child("rooms").child(roomCode).child("queue")
        queueRef.runTransactionAwait { currentData ->
            val currentQueue = currentData.children
                .mapNotNull { it.getValue(Song::class.java) }
                .toMutableList()
            if (index !in currentQueue.indices) {
                Transaction.abort()
            } else {
                currentQueue.removeAt(index)
                currentData.value = currentQueue
                Transaction.success(currentData)
            }
        }
    }

    suspend fun addToQueue(roomCode: String, song: Song) {
        requireParticipant(roomCode)
        val queueRef = database.child("rooms").child(roomCode).child("queue")
        queueRef.runTransactionAwait { currentData ->
            val currentQueue = currentData.children
                .mapNotNull { it.getValue(Song::class.java) }
                .toMutableList()
            currentQueue.add(song)
            currentData.value = currentQueue
            Transaction.success(currentData)
        }
    }

    fun observeRoom(roomCode: String): Flow<JamRoom?> = callbackFlow {
        requireOnline()
        val ref = database.child("rooms").child(roomCode)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = try {
                    snapshot.getValue(JamRoom::class.java)
                } catch (e: Exception) {
                    // Ignore rooms written by incompatible older clients.
                    e.printStackTrace()
                    null
                }
                trySend(room)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun updatePlaybackState(roomCode: String, state: JamPlaybackState) {
        requireHost(roomCode)
        database.child("rooms").child(roomCode).child("playback")
            .setValue(state)
            .await()
    }

    suspend fun voteSkip(roomCode: String) {
        requireParticipant(roomCode)
        database.child("rooms").child(roomCode).child("skipVotes")
            .child(currentUserId)
            .setValue(true)
            .await()
    }

    suspend fun resetSkipVotes(roomCode: String) {
        requireHost(roomCode)
        database.child("rooms").child(roomCode).child("skipVotes")
            .removeValue()
            .await()
    }

    /**
     * Firebase's Android SDK exposes transactions as a callback API. Bridge the
     * callback into a suspend function so callers can await the committed result.
     */
    private suspend fun DatabaseReference.runTransactionAwait(
        doTransaction: (MutableData) -> Transaction.Result
    ): Boolean = suspendCancellableCoroutine { continuation ->
        runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result =
                doTransaction(currentData)

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (!continuation.isActive) return
                if (error != null) {
                    continuation.resumeWith(Result.failure(error.toException()))
                } else {
                    continuation.resumeWith(Result.success(committed))
                }
            }
        })
    }

    private suspend fun requireAuthenticated() {
        if (currentUserId.isBlank()) authenticateAnonymously()
        check(currentUserId.isNotBlank()) { "Pulse JAM authentication failed." }
    }

    private suspend fun requireHost(roomCode: String) {
        requireAuthenticated()
        val hostId = database.child("rooms").child(roomCode).child("hostId").get().await()
            .getValue(String::class.java)
        check(hostId == currentUserId) { "Only the JAM host can perform this action." }
    }

    private suspend fun requireParticipant(roomCode: String) {
        requireAuthenticated()
        val participant = database.child("rooms").child(roomCode)
            .child("participants")
            .child(currentUserId)
            .get()
            .await()
        check(participant.exists()) { "Join the JAM room before performing this action." }
    }

    companion object {
        private const val MAX_PARTICIPANTS = 8
    }
}
