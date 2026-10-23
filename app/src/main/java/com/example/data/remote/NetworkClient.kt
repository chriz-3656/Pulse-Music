package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkClient {
    const val DEFAULT_BASE_URL = "https://music-api2.albatross0071.workers.dev/api/"
    const val MIRROR_BASE_URL = "https://saavn.dev/api/"
    private const val CACHE_SIZE = 20L * 1024 * 1024 // 20 MB

    fun createMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun createApiOkHttpClient(context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_api_cache")
        val cache = Cache(cacheDir, CACHE_SIZE)
        val prefs: SharedPreferences = context.getSharedPreferences("pulse_music_settings", Context.MODE_PRIVATE)

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val offlineGuard = Interceptor { chain ->
            if (prefs.getBoolean("is_pure_offline", false)) {
                throw IOException("Pure Offline Mode is enabled")
            }
            chain.proceed(chain.request())
        }

        // Dynamic Host & Custom Endpoint Interceptor ONLY for API requests
        val dynamicHostInterceptor = Interceptor { chain ->
            var request = chain.request()
            val customUrl = prefs.getString("custom_api_url", null)?.trim()
            val originalHost = request.url.host

            // Only redirect if custom URL is provided AND request was targeting an API host
            val isApiTarget = originalHost.contains("albatross0071") ||
                    originalHost.contains("saavn.dev") ||
                    request.url.encodedPath.startsWith("/api")

            if (!customUrl.isNullOrBlank() && isApiTarget) {
                buildCustomApiUrl(request.url, customUrl)?.let { newUrl ->
                    request = request.newBuilder()
                        .url(newUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        .build()
                }
            }
            chain.proceed(request)
        }

        val safeRetryInterceptor = Interceptor { chain ->
            val request = chain.request()
            var response: Response? = null
            var lastException: IOException? = null
            var tryCount = 0
            val maxRetries = 2

            while (tryCount <= maxRetries) {
                try {
                    response?.close()
                    response = chain.proceed(request)
                    return@Interceptor response
                } catch (e: IOException) {
                    lastException = e
                    tryCount++
                    if (tryCount <= maxRetries) {
                        try {
                            Thread.sleep(400L * tryCount)
                        } catch (ie: InterruptedException) {
                            Thread.currentThread().interrupt()
                            break
                        }
                    }
                }
            }

            throw (lastException ?: IOException("Network request failed after retries"))
        }

        val cacheInterceptor = Interceptor { chain ->
            val request = chain.request()
            try {
                val response = chain.proceed(request)
                if (request.method == "GET" && response.isSuccessful) {
                    val cacheControl = CacheControl.Builder()
                        .maxAge(3, TimeUnit.MINUTES)
                        .build()
                    response.newBuilder()
                        .header("Cache-Control", cacheControl.toString())
                        .removeHeader("Pragma")
                        .build()
                } else {
                    response
                }
            } catch (e: Exception) {
                throw e
            }
        }

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(offlineGuard)
            .addInterceptor(dynamicHostInterceptor)
            .addInterceptor(safeRetryInterceptor)
            .addInterceptor(logging)
            .addNetworkInterceptor(cacheInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun createMediaOkHttpClient(context: Context): OkHttpClient {
        val prefs: SharedPreferences = context.getSharedPreferences("pulse_music_settings", Context.MODE_PRIVATE)
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val offlineGuard = Interceptor { chain ->
            if (prefs.getBoolean("is_pure_offline", false)) {
                throw IOException("Pure Offline Mode is enabled")
            }
            chain.proceed(chain.request())
        }

        val userAgentInterceptor = Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "*/*")
                .header("Connection", "keep-alive")
                .build()
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(offlineGuard)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Builds the effective API URL by combining the configured base URL with the
     * original request path. The base path is preserved, and a duplicated first
     * segment (for example, `/api` + `/api/search`) is removed.
     */
    internal fun buildCustomApiUrl(originalUrl: HttpUrl, customUrl: String): HttpUrl? {
        val target = customUrl.trim().toHttpUrlOrNull() ?: return null
        if (!target.scheme.equals("https", ignoreCase = true)) return null

        val mergedSegments = target.pathSegments
            .filter { it.isNotEmpty() }
            .toMutableList()
        for (segment in originalUrl.pathSegments.filter { it.isNotEmpty() }) {
            if (mergedSegments.isEmpty() ||
                !mergedSegments.last().equals(segment, ignoreCase = true)
            ) {
                mergedSegments.add(segment)
            }
        }

        return originalUrl.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .encodedPath("/")
            .apply { mergedSegments.forEach { segment -> addPathSegment(segment) } }
            .build()
    }

    fun createOkHttpClient(context: Context): OkHttpClient {
        return createApiOkHttpClient(context)
    }

    fun createApiService(okHttpClient: OkHttpClient, moshi: Moshi): MusicApiService {
        return Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MusicApiService::class.java)
    }
}
