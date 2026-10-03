import re

with open('app/src/main/java/com/example/data/player/MediaCacheManager.kt', 'r') as f:
    text = f.read()

# Replace LeastRecentlyUsedCacheEvictor with DynamicCacheEvictor
text = text.replace('import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor', '')

old_evictor = """    // Default max cache size 1GB
    private var maxCacheSizeBytes = 1000L * 1024 * 1024
    private var evictor = LeastRecentlyUsedCacheEvictor(maxCacheSizeBytes)"""
new_evictor = """    // Default max cache size 1GB
    private var maxCacheSizeBytes = 1000L * 1024 * 1024
    private var evictor = DynamicCacheEvictor(maxCacheSizeBytes)"""
text = text.replace(old_evictor, new_evictor)

old_limit = """    fun setCacheLimitMb(limitMb: Int) {
        maxCacheSizeBytes = limitMb.toLong() * 1024 * 1024
    }"""
new_limit = """    fun setCacheLimitMb(limitMb: Int) {
        maxCacheSizeBytes = limitMb.toLong() * 1024 * 1024
        evictor.setMaxBytes(maxCacheSizeBytes)
    }"""
text = text.replace(old_limit, new_limit)

with open('app/src/main/java/com/example/data/player/MediaCacheManager.kt', 'w') as f:
    f.write(text)
