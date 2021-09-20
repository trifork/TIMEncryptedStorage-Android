package com.trifork.timencryptedstorage.keyservice

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType


internal object JsonConverter {
    private val builder = Json {
        ignoreUnknownKeys = true
    }

    @OptIn(ExperimentalSerializationApi::class)
    val factory = builder.asConverterFactory("application/json".toMediaType())
}