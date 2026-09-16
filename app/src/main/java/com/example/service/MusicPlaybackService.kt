package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.PulseMusicApplication
import com.example.domain.model.AudioQuality
import com.example.domain.model.RepeatMode
import com.example.domain.model.Song
import com.example.player.MusicPlayerController
import com.example.player.ServiceAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class MusicPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "pulse_music_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_PREV = "com.example.ACTION_PREV"
        const val ACTION_STOP = "com.example.ACTION_STOP"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat

    private val playerController: MusicPlayerController by lazy {
        (applicationContext as PulseMusicApplication).appContainer.playerController
    }

    private val cacheManager by lazy {
        (applicationContext as PulseMusicApplication).appContainer.mediaCacheManager
    }

    private val musicRepository by lazy {
        (applicationContext as PulseMusicApplication).appContainer.musicRepository
    }

    private var currentArtworkBitmap: Bitmap? = null
    private var isForegroundActive = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initMediaSession()
        initExoPlayer()

        playerController.onServiceCommand = { action ->
            handleServiceAction(action)
        }

        // Start foreground immediately to satisfy Android 12+ requirements
        val initialNotification = buildInitialNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
            isForegroundActive = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "PulseMusicMediaSession").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    resumePlayback()
                }

                override fun onPause() {
                    pausePlayback()
                }

                override fun onSkipToNext() {
                    playerController.skipToNext()
                }

                override fun onSkipToPrevious() {
                    playerController.skipToPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos)
                }

                override fun onStop() {
                    stopPlayback()
                }
            })
            isActive = true
        }
    }

    private fun initExoPlayer() {
        val cacheDataSourceFactory = cacheManager.createCacheDataSourceFactory()
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                val isPlaying = exoPlayer.isPlaying
                playerController.updatePlaybackState(isPlaying, isBuffering)

                if (playbackState == Player.STATE_READY) {
                    val duration = exoPlayer.duration
                    if (duration > 0) {
                        playerController.updateProgress(exoPlayer.currentPosition, duration, exoPlayer.bufferedPosition)
                    }
                    updateNotification()
                } else if (playbackState == Player.STATE_ENDED) {
                    playerController.skipToNext()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playerController.updatePlaybackState(isPlaying, exoPlayer.playbackState == Player.STATE_BUFFERING)
                updateMediaSessionState()
                updateNotification()
                if (isPlaying) {
                    startProgressPolling()
                } else {
                    stopProgressPolling()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val errorMsg = error.localizedMessage ?: "Playback error: ${error.errorCodeName}"
                playerController.updatePlaybackState(isPlaying = false, isBuffering = false, error = errorMsg)

                serviceScope.launch {
                    delay(1500)
                    playerController.skipToNext()
                }
            }
        })
    }

    private fun handleServiceAction(action: ServiceAction) {
        when (action) {
            is ServiceAction.PlayTrack -> {
                playTrack(action.song)
            }
            is ServiceAction.Resume -> {
                resumePlayback()
            }
            is ServiceAction.Pause -> {
                pausePlayback()
            }
            is ServiceAction.SeekTo -> {
                seekTo(action.positionMs)
            }
            is ServiceAction.SetShuffle -> {
                exoPlayer.shuffleModeEnabled = action.isShuffle
            }
            is ServiceAction.SetRepeat -> {
                exoPlayer.repeatMode = when (action.mode) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                }
            }
            is ServiceAction.ReloadStreamWithQuality -> {
                val song = playerController.playerState.value.currentSong
                if (song != null) {
                    serviceScope.launch {
                        val preferHigh = action.quality == AudioQuality.HIGH
                        var url = song.getStreamUrl(preferHigh)
                        if (url.isBlank()) {
                            url = musicRepository.resolveStreamUrl(song)
                            if (url.isNotBlank()) {
                                val updated = song.copy(stream160Url = url, stream320Url = url)
                                playerController.updateCurrentSong(updated)
                            }
                        }
                        if (url.isNotBlank()) {
                            withContext(Dispatchers.Main) {
                                val mediaItem = MediaItem.fromUri(Uri.parse(url))
                                exoPlayer.setMediaItem(mediaItem)
                                exoPlayer.seekTo(action.currentPos)
                                exoPlayer.prepare()
                                exoPlayer.play()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun playTrack(song: Song) {
        playerController.updatePlaybackState(isPlaying = true, isBuffering = true, error = null)

        serviceScope.launch(Dispatchers.IO) {
            var activeSong = song
            val quality = playerController.playerState.value.audioQuality

            // PRIORITY 1: Check if track is available locally for instant offline playback
            var localFile: java.io.File? = null
            if (!activeSong.localFilePath.isNullOrBlank()) {
                val f = java.io.File(activeSong.localFilePath!!)
                if (f.exists() && f.length() > 0L) {
                    localFile = f
                }
            }
            if (localFile == null) {
                val downloadedPath = musicRepository.getLocalDownloadPath(activeSong.id)
                if (!downloadedPath.isNullOrBlank()) {
                    val f = java.io.File(downloadedPath)
                    if (f.exists() && f.length() > 0L) {
                        localFile = f
                    }
                }
            }

            var streamUrl = ""
            if (localFile != null) {
                streamUrl = Uri.fromFile(localFile).toString()
                activeSong = activeSong.copy(
                    isDownloaded = true,
                    localFilePath = localFile.absolutePath,
                    stream160Url = streamUrl,
                    stream320Url = streamUrl
                )
                withContext(Dispatchers.Main) {
                    playerController.updateCurrentSong(activeSong)
                }
            } else {
                streamUrl = activeSong.getStreamUrl(preferHighQuality = quality == AudioQuality.HIGH)
            }

            // 1. If streamUrl is empty, first try resolving stream URL directly (JioSaavn / SoundCloud / YTM fallback)
            if (streamUrl.isBlank()) {
                try {
                    val resolved = musicRepository.resolveStreamUrl(activeSong)
                    if (resolved.isNotBlank()) {
                        streamUrl = resolved
                        activeSong = activeSong.copy(
                            stream160Url = resolved,
                            stream320Url = resolved
                        )
                        withContext(Dispatchers.Main) {
                            playerController.updateCurrentSong(activeSong)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. If still empty, try fetching full song details from repository
            if (streamUrl.isBlank()) {
                try {
                    val result = musicRepository.getSongDetails(song.id)
                    if (result.isSuccess) {
                        val fetchedSong = result.getOrNull()
                        if (fetchedSong != null) {
                            activeSong = fetchedSong
                            streamUrl = activeSong.getStreamUrl(preferHighQuality = quality == AudioQuality.HIGH)
                            withContext(Dispatchers.Main) {
                                playerController.updateCurrentSong(activeSong)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // If still blank, we can't play it
            if (streamUrl.isBlank()) {
                withContext(Dispatchers.Main) {
                    playerController.updatePlaybackState(isPlaying = false, isBuffering = false, error = "No stream available for ${song.title}")
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                try {
                    val mediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(streamUrl))
                        .setMediaId(activeSong.id)
                        .build()

                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.play()

                    loadArtwork(activeSong.artworkUrl)
                    updateMediaMetadata(activeSong)
                    updateMediaSessionState()
                    
                    val notification = buildNotification(activeSong, isPlaying = true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    isForegroundActive = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    playerController.updatePlaybackState(isPlaying = false, isBuffering = false, error = e.localizedMessage)
                }
            }
        }
    }

    private fun resumePlayback() {
        exoPlayer.play()
    }

    private fun pausePlayback() {
        exoPlayer.pause()
    }

    private fun seekTo(pos: Long) {
        exoPlayer.seekTo(pos)
        updateMediaSessionState()
    }

    private fun stopPlayback() {
        exoPlayer.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForegroundActive = false
        stopSelf()
    }

    private fun loadArtwork(url: String) {
        currentArtworkBitmap = null
        if (url.isBlank()) {
            return
        }

        serviceScope.launch(Dispatchers.IO) {
            try {
                val loader = ImageLoader(this@MusicPlaybackService)
                val request = ImageRequest.Builder(this@MusicPlaybackService)
                    .data(url)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    currentArtworkBitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    withContext(Dispatchers.Main) {
                        updateNotification()
                        updateMediaMetadata(playerController.playerState.value.currentSong!!)
                    }
                }
            } catch (e: Exception) {
                currentArtworkBitmap = null
            }
        }
    }

    private fun updateMediaMetadata(song: Song) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.durationSec * 1000L)
            .apply {
                if (currentArtworkBitmap != null) {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentArtworkBitmap)
                }
            }
            .build()
        mediaSession.setMetadata(metadata)
    }

    private fun updateMediaSessionState() {
        val state = if (exoPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, exoPlayer.currentPosition, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val cur = exoPlayer.currentPosition
                    val dur = exoPlayer.duration
                    val buf = exoPlayer.bufferedPosition
                    playerController.updateProgress(cur, dur, buf)
                }
                delay(250)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pulse Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows now playing music notification with controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildInitialNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Pulse Music")
            .setContentText("Ready for playback")
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .build()
    }

    private fun buildNotification(song: Song, isPlaying: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPendingIntent = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIntent = Intent(this, MusicPlaybackService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePendingIntent = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(song.title)
            .setContentText("${song.artist} • ${song.album}")
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        currentArtworkBitmap?.let {
            builder.setLargeIcon(it)
        }

        return builder.build()
    }

    private fun updateNotification() {
        val song = playerController.playerState.value.currentSong ?: return
        val notification = buildNotification(song, exoPlayer.isPlaying)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> resumePlayback()
            ACTION_PAUSE -> pausePlayback()
            ACTION_NEXT -> playerController.skipToNext()
            ACTION_PREV -> playerController.skipToPrevious()
            ACTION_STOP -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return ServiceBinder()
    }

    inner class ServiceBinder : Binder() {
        val service: MusicPlaybackService get() = this@MusicPlaybackService
    }

    override fun onDestroy() {
        stopProgressPolling()
        exoPlayer.release()
        mediaSession.release()
        super.onDestroy()
    }
}
