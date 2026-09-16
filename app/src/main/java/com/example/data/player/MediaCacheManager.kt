package com.example.data.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.io.File

@OptIn(UnstableApi::class)
class MediaCacheManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val databaseProvider = StandaloneDatabaseProvider(context)
    private val cacheFolder = File(context.cacheDir, "exoplayer_media_cache")
    
    // Default max cache size 1GB
    private var maxCacheSizeBytes = 1000L * 1024 * 1024
    private var evictor = LeastRecentlyUsedCacheEvictor(maxCacheSizeBytes)

    val simpleCache: SimpleCache by lazy {
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs()
        }
        SimpleCache(cacheFolder, evictor, databaseProvider)
    }

    fun setCacheLimitMb(limitMb: Int) {
        maxCacheSizeBytes = limitMb.toLong() * 1024 * 1024
    }

    fun getUsedCacheSizeBytes(): Long {
        return try {
            simpleCache.cacheSpace
        } catch (e: Exception) {
            0L
        }
    }

    fun clearMediaCache() {
        try {
            val keys = simpleCache.keys
            for (key in keys) {
                simpleCache.removeResource(key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createCacheDataSourceFactory(): DataSource.Factory {
        val upstreamFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
        
        val defaultDataSourceFactory = DefaultDataSource.Factory(context, upstreamFactory)

        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
