package com.trifork.timencryptedstorage.keyservice

import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal object TIMCustomOkHttp {

    private var client: OkHttpClient? = null

    private fun createTIMOkHttpBuilder(): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        // Force HTTPS
        builder.connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))

        return builder
    }

    fun getClient(): OkHttpClient {
        return client ?: createTIMOkHttpBuilder().build().apply {
            client = this
        }
    }

}