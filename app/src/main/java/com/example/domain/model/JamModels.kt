package com.example.domain.model

data class JamRoom(
    val roomId: String = "",
    val hostId: String = "",
    val settings: JamSettings = JamSettings(),
    val playback: JamPlaybackState = JamPlaybackState(),
    val queue: List<Song> = emptyList(),
    val participants: Map<String, JamParticipant> = emptyMap(),
    val skipVotes: List<String> = emptyList()
)

data class JamSettings(
    val playbackControl: String = "HOST_ONLY", // "HOST_ONLY", "EVERYONE", "VOTE"
    val queueControl: String = "EVERYONE"
)

data class JamPlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val updatedAt: Long = 0L
)

data class JamParticipant(
    val name: String = "",
    val isHost: Boolean = false
)
