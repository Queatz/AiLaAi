package com.queatz

import com.queatz.plugins.secrets
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.text.Charsets.UTF_8
import kotlin.time.Duration.Companion.minutes

@Serializable
data class OpenAiSpeakBody(
    val input: String,
    val model: String = "tts-1-hd",
    val voice: String = "shimmer",
    @SerialName("response_format") val format: String = "response_format",
    val speed: Double = 1.0
)

class OpenAi {

    val http = HttpClient(CIO) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(com.queatz.plugins.json)
        }

        engine {
            requestTimeout = 4.minutes.inWholeMilliseconds
        }
    }


    suspend fun speak(text: String): ByteArrayContent? = runCatching {
        http.post("https://api.openai.com/v1/audio/generate") {
            bearerAuth(secrets.openAi.key)
            contentType(ContentType.Application.Json.withCharset(UTF_8))
            setBody(
                OpenAiSpeakBody(input = text)
            )
        }.body<ByteArrayContent>()
    }.onFailure {
        it.printStackTrace()
    }.getOrNull()
}
