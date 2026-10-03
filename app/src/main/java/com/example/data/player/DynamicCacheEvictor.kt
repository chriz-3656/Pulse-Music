package com.example.data.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan
import java.util.TreeSet

@UnstableApi
class DynamicCacheEvictor(private var maxBytes: Long) : CacheEvictor {
    private val leastRecentlyUsed = TreeSet<CacheSpan> { span1, span2 ->
        val lastTouchTimestampDelta = span1.lastTouchTimestamp - span2.lastTouchTimestamp
        if (lastTouchTimestampDelta == 0L) {
            span1.compareTo(span2)
        } else {
            if (lastTouchTimestampDelta < 0) -1 else 1
        }
    }
    private var currentSize: Long = 0

    fun setMaxBytes(maxBytes: Long) {
        this.maxBytes = maxBytes
        // Note: we can't easily trigger eviction here without a reference to the Cache.
        // It will evict on the next onSpanAdded.
    }

    override fun requiresCacheSpanTouches(): Boolean = true

    override fun onCacheInitialized() {
        // Do nothing
    }

    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        evictCache(cache, length)
    }

    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.add(span)
        currentSize += span.length
        evictCache(cache, 0)
    }

    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        leastRecentlyUsed.remove(span)
        currentSize -= span.length
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    private fun evictCache(cache: Cache, requiredSpace: Long) {
        while (currentSize + requiredSpace > maxBytes && !leastRecentlyUsed.isEmpty()) {
            try {
                cache.removeSpan(leastRecentlyUsed.first())
            } catch (e: Cache.CacheException) {
                // Ignore.
            }
        }
    }
}
