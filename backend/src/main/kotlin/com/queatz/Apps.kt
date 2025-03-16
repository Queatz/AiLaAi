package com.queatz

import com.queatz.plugins.json
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlin.text.Charsets.UTF_8
import kotlin.time.Duration.Companion.minutes

class Apps {
    private lateinit var coroutineScope: CoroutineScope
    private val http = HttpClient(CIO) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }

        engine {
            requestTimeout = 1.minutes.inWholeMilliseconds
        }
    }

    fun start(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
    }


}

private fun HttpRequestBuilder.json() {
    contentType(ContentType.Application.Json.withCharset(UTF_8))
}
