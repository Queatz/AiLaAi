package com.queatz.scripts

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.text.Charsets.UTF_8
import kotlin.time.Duration.Companion.seconds

private val json = Json {
    encodeDefaults = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    ignoreUnknownKeys = true
    explicitNulls = false
}

val scriptHttpClient = HttpClient(Java) {
    engine {
        protocolVersion = java.net.http.HttpClient.Version.HTTP_2
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30.seconds.inWholeMilliseconds
    }
    install(ContentNegotiation) {
        expectSuccess = true
        json(json)
    }
}

class ScriptHttp {

    inline operator fun <reified T> invoke(url: String): T = runBlocking { scriptHttpClient.get(url).body() }

    inline fun <reified T> post(url: String): T = runBlocking { scriptHttpClient.post(url).body() }

    inline fun <reified T, reified B> post(url: String, body: B): T = runBlocking {
        scriptHttpClient.post(url) {
            contentType(ContentType.Application.Json.withCharset(UTF_8))
            setBody(body)
        }.body()
    }
}
