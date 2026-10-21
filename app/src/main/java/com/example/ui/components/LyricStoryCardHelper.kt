package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object LyricStoryCardHelper {

    fun generateStoryCardBitmap(
        context: Context,
        song: Song,
        artworkBitmap: Bitmap?,
        selectedLyrics: List<String>
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Deep Obsidian / Onyx Gradient with warm amber vignette
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    Color.rgb(20, 20, 24),
                    Color.rgb(28, 20, 14),
                    Color.rgb(10, 10, 12)
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Top Header Brand Bar
        val headerPaint = Paint().apply {
            color = Color.rgb(245, 158, 11) // Amber primary
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.2f
        }
        canvas.drawText("PULSE MUSIC • ANALOG DECK", width / 2f, 160f, headerPaint)

        // 3. Album Artwork / Center Stage
        val artSize = 440
        val artTop = 220f
        val artLeft = (width - artSize) / 2f
        val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)

        if (artworkBitmap != null) {
            val roundedArt = getRoundedCroppedBitmap(artworkBitmap, artSize, 36f)
            canvas.drawBitmap(roundedArt, artLeft, artTop, null)
        } else {
            // Stylized Vinyl Record Graphic
            val vinylPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(28, 28, 32) }
            canvas.drawCircle(width / 2f, artTop + artSize / 2f, artSize / 2f, vinylPaint)
            val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(245, 158, 11) }
            canvas.drawCircle(width / 2f, artTop + artSize / 2f, 40f, centerPaint)
        }

        // Border around artwork
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(artRect, 36f, 36f, borderPaint)

        // 4. Song Title & Artist
        var currentY = artTop + artSize + 70f
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val safeTitle = if (song.title.length > 36) song.title.take(33) + "..." else song.title
        canvas.drawText(safeTitle, width / 2f, currentY, titlePaint)

        currentY += 50f
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(245, 158, 11) // Amber accent
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val safeArtist = if (song.artist.length > 40) song.artist.take(37) + "..." else song.artist
        canvas.drawText(safeArtist, width / 2f, currentY, artistPaint)

        // 5. Selected Lyrics Card / Quotes
        currentY += 80f
        val lyricsCardWidth = 900f
        val lyricsCardHeight = 640f
        val lyricsCardLeft = (width - lyricsCardWidth) / 2f
        val cardRect = RectF(lyricsCardLeft, currentY, lyricsCardLeft + lyricsCardWidth, currentY + lyricsCardHeight)

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 15, 15, 18)
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 245, 158, 11)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBorderPaint)

        // Opening Quote Graphic
        val quoteMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 245, 158, 11)
            textSize = 90f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText("“", lyricsCardLeft + 40f, currentY + 90f, quoteMarkPaint)

        // Lyric Text Lines
        val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249)
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val lyricContent = selectedLyrics.joinToString("\n")
        val textWidth = (lyricsCardWidth - 100).toInt()
        val staticLayout = StaticLayout.Builder.obtain(lyricContent, 0, lyricContent.length, lyricPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(14f, 1.15f)
            .build()

        val textTop = currentY + 120f
        canvas.save()
        canvas.translate(lyricsCardLeft + 50f, textTop)
        staticLayout.draw(canvas)
        canvas.restore()

        // 6. Watermark Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 148, 163, 184)
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.15f
        }
        canvas.drawText("LISTEN ON PULSE MUSIC", width / 2f, height - 120f, footerPaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(245, 158, 11)
        }
        canvas.drawCircle(width / 2f - 210f, height - 128f, 6f, dotPaint)
        canvas.drawCircle(width / 2f + 210f, height - 128f, 6f, dotPaint)

        return bitmap
    }

    private fun getRoundedCroppedBitmap(bitmap: Bitmap, size: Int, cornerRadius: Float): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = Rect(0, 0, size, size)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        return output
    }

    suspend fun loadArtworkBitmap(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                return@withContext (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    fun shareStoryCard(context: Context, bitmap: Bitmap, songTitle: String, artist: String) {
        try {
            val storiesDir = File(context.cacheDir, "shared_stories").apply { mkdirs() }
            val file = File(storiesDir, "pulse_story_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Listening to \"$songTitle\" by $artist on Pulse Music 🎵")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Lyric Story"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun LyricStoryPreviewDialog(
    song: Song,
    selectedLyrics: List<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var storyBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(true) }

    LaunchedEffect(song, selectedLyrics) {
        val artwork = LyricStoryCardHelper.loadArtworkBitmap(context, song.artworkUrl)
        val card = LyricStoryCardHelper.generateStoryCardBitmap(context, song, artwork, selectedLyrics)
        storyBitmap = card
        isGenerating = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.background)
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STORY CARD PREVIEW",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isGenerating || storyBitmap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Image(
                        bitmap = storyBitmap!!.asImageBitmap(),
                        contentDescription = "Lyric Story Card",
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SkeuoTactileButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }

                    SkeuoTactileButton(
                        onClick = {
                            storyBitmap?.let { bmp ->
                                LyricStoryCardHelper.shareStoryCard(context, bmp, song.title, song.artist)
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1.5f).height(42.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SHARE TO STORY", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
