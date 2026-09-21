package com.example.util

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NpRequest
import org.schabi.newpipe.extractor.downloader.Response as NpResponse
import java.util.concurrent.TimeUnit

class NewPipeDownloader : Downloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun execute(request: NpRequest): NpResponse {
        val reqBuilder = Request.Builder().url(request.url())
        
        request.headers().forEach { (k, v) ->
            v.forEach { headerVal ->
                reqBuilder.addHeader(k, headerVal)
            }
        }
        
        if (request.httpMethod() == "POST") {
            reqBuilder.post((request.dataToSend()?.joinToString("") ?: "").toRequestBody())
        }

        val response = client.newCall(reqBuilder.build()).execute()
        val headers = response.headers.toMultimap().mapValues { it.value.toList() }
        
        return NpResponse(
            response.code,
            response.message,
            headers,
            response.body?.string() ?: "",
            response.request.url.toString()
        )
    }
}
