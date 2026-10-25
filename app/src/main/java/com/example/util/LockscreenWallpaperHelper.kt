package com.example.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Manages rendering and setting a slightly zoomed, blurred full-screen album artwork
 * as the Android native lock screen wallpaper (via WallpaperManager.FLAG_LOCK).
 */
class LockscreenWallpaperHelper(private val context: Context) {

    private val wallpaperManager: WallpaperManager by lazy {
        WallpaperManager.getInstance(context.applicationContext)
    }

    @Volatile
    private var hasSetWallpaper: Boolean = false

    @Volatile
    private var lastAppliedArtworkHash: Int? = null

    /**
     * Blurs, zooms, and sets the given artwork as the lock screen wallpaper.
     */
    suspend fun setLockscreenWallpaper(artwork: Bitmap) = withContext(Dispatchers.IO) {
        val currentHash = artwork.generationId
        if (hasSetWallpaper && lastAppliedArtworkHash == currentHash) {
            return@withContext
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return@withContext
        }

        try {
            if (!wallpaperManager.isWallpaperSupported || !wallpaperManager.isSetWallpaperAllowed) {
                return@withContext
            }

            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            if (screenWidth <= 0 || screenHeight <= 0) return@withContext

            val blurredWallpaper = createBlurredZoomedWallpaper(
                source = artwork,
                targetWidth = screenWidth,
                targetHeight = screenHeight
            ) ?: return@withContext

            wallpaperManager.setBitmap(
                blurredWallpaper,
                null,
                false,
                WallpaperManager.FLAG_LOCK
            )

            hasSetWallpaper = true
            lastAppliedArtworkHash = currentHash
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clears the dynamic lock screen wallpaper and restores the user's default wallpaper.
     */
    suspend fun clearLockscreenWallpaper() = withContext(Dispatchers.IO) {
        if (!hasSetWallpaper) return@withContext

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && wallpaperManager.isWallpaperSupported) {
                wallpaperManager.clear(WallpaperManager.FLAG_LOCK)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            hasSetWallpaper = false
            lastAppliedArtworkHash = null
        }
    }

    /**
     * Creates a slightly zoomed and heavily blurred wallpaper matching the device screen aspect ratio.
     * Includes a subtle translucent dark scrim so the lock screen clock and notifications remain crisp.
     */
    private fun createBlurredZoomedWallpaper(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        return try {
            // Downscale intermediate canvas for high-performance, silky smooth blur (~360px width)
            val downscaleFactor = max(1f, targetWidth / 360f)
            val intermediateW = (targetWidth / downscaleFactor).toInt().coerceAtLeast(100)
            val intermediateH = (targetHeight / downscaleFactor).toInt().coerceAtLeast(100)

            val intermediateBitmap = Bitmap.createBitmap(intermediateW, intermediateH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(intermediateBitmap)

            // Calculate slightly zoomed center-crop transformation (1.12x zoom for full-bleed immersion)
            val zoomFactor = 1.12f
            val scaleX = (intermediateW.toFloat() / source.width) * zoomFactor
            val scaleY = (intermediateH.toFloat() / source.height) * zoomFactor
            val baseScale = max(scaleX, scaleY)

            val matrix = Matrix().apply {
                postScale(baseScale, baseScale)
                val scaledW = source.width * baseScale
                val scaledH = source.height * baseScale
                postTranslate(
                    (intermediateW - scaledW) / 2f,
                    (intermediateH - scaledH) / 2f
                )
            }

            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
            canvas.drawBitmap(source, matrix, paint)

            // Apply fast stack blur algorithm (radius 22)
            val blurredIntermediate = fastStackBlur(intermediateBitmap, 22) ?: intermediateBitmap

            // Render to full resolution target with subtle dark tint for text contrast
            val finalWallpaper = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val finalCanvas = Canvas(finalWallpaper)

            val upscaleMatrix = Matrix().apply {
                postScale(
                    targetWidth.toFloat() / blurredIntermediate.width,
                    targetHeight.toFloat() / blurredIntermediate.height
                )
            }
            finalCanvas.drawBitmap(blurredIntermediate, upscaleMatrix, paint)

            // 28% translucent dark overlay for status bar & lockscreen clock readability
            finalCanvas.drawColor(Color.argb(72, 0, 0, 0))

            finalWallpaper
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * In-memory StackBlur algorithm by Mario Klingemann.
     * Efficient O(N) multi-pass blur that does not rely on deprecated RenderScript.
     */
    private fun fastStackBlur(sentBitmap: Bitmap, radius: Int): Bitmap? {
        if (radius < 1) return null

        val w = sentBitmap.width
        val h = sentBitmap.height
        val pix = IntArray(w * h)
        sentBitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        var yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val routsum = IntArray(1)
        val goutsum = IntArray(1)
        val boutsum = IntArray(1)
        val rinsum = IntArray(1)
        val ginsum = IntArray(1)
        val binsum = IntArray(1)

        for (row in 0 until h) {
            rinsum[0] = 0; ginsum[0] = 0; binsum[0] = 0
            routsum[0] = 0; goutsum[0] = 0; boutsum[0] = 0
            rsum = 0; gsum = 0; bsum = 0

            for (col in -radius..radius) {
                p = pix[yi + col.coerceIn(0, wm)]
                sir = stack[col + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)

                rbs = radius + 1 - kotlin.math.abs(col)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs

                if (col > 0) {
                    rinsum[0] += sir[0]
                    ginsum[0] += sir[1]
                    binsum[0] += sir[2]
                } else {
                    routsum[0] += sir[0]
                    goutsum[0] += sir[1]
                    boutsum[0] += sir[2]
                }
            }
            stackpointer = radius

            for (col in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum[0]
                gsum -= goutsum[0]
                bsum -= boutsum[0]

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum[0] -= sir[0]
                goutsum[0] -= sir[1]
                boutsum[0] -= sir[2]

                if (row == 0) {
                    vmin[col] = (col + radius + 1).coerceAtMost(wm)
                }
                p = pix[yw + vmin[col]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)

                rinsum[0] += sir[0]
                ginsum[0] += sir[1]
                binsum[0] += sir[2]

                rsum += rinsum[0]
                gsum += ginsum[0]
                bsum += binsum[0]

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum[0] += sir[0]
                goutsum[0] += sir[1]
                boutsum[0] += sir[2]

                rinsum[0] -= sir[0]
                ginsum[0] -= sir[1]
                binsum[0] -= sir[2]

                yi++
            }
            yw += w
        }

        for (col in 0 until w) {
            rinsum[0] = 0; ginsum[0] = 0; binsum[0] = 0
            routsum[0] = 0; goutsum[0] = 0; boutsum[0] = 0
            rsum = 0; gsum = 0; bsum = 0
            yp = -radius * w

            for (row in -radius..radius) {
                yi = max(0, yp) + col
                sir = stack[row + radius]

                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]

                rbs = radius + 1 - kotlin.math.abs(row)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs

                if (row > 0) {
                    rinsum[0] += sir[0]
                    ginsum[0] += sir[1]
                    binsum[0] += sir[2]
                } else {
                    routsum[0] += sir[0]
                    goutsum[0] += sir[1]
                    boutsum[0] += sir[2]
                }

                if (row < hm) {
                    yp += w
                }
            }
            yi = col
            stackpointer = radius

            for (row in 0 until h) {
                pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum[0]
                gsum -= goutsum[0]
                bsum -= boutsum[0]

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum[0] -= sir[0]
                goutsum[0] -= sir[1]
                boutsum[0] -= sir[2]

                if (col == 0) {
                    vmin[row] = (row + radius + 1).coerceAtMost(hm) * w
                }
                p = col + vmin[row]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum[0] += sir[0]
                ginsum[0] += sir[1]
                binsum[0] += sir[2]

                rsum += rinsum[0]
                gsum += ginsum[0]
                bsum += binsum[0]

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum[0] += sir[0]
                goutsum[0] += sir[1]
                boutsum[0] += sir[2]

                rinsum[0] -= sir[0]
                ginsum[0] -= sir[1]
                binsum[0] -= sir[2]

                yi += w
            }
        }

        return Bitmap.createBitmap(pix, 0, w, w, h, Bitmap.Config.ARGB_8888)
    }
}
