package com.queatz

import com.queatz.plugins.json
import com.queatz.plugins.secrets
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class GoogleGenerateImagesBody(
    val instances: List<GoogleImageInstance>,
    val parameters: GoogleImageParameters
)

@Serializable
data class GoogleImageInstance(
    val prompt: String
)

@Serializable
data class GoogleImageParameters(
    @SerialName("sampleCount")
    val sampleCount: Int = 1,
    @SerialName("outputMimeType")
    val outputMimeType: String = "image/jpeg"
)

@Serializable
data class GoogleGenerateImagesResponse(
    val predictions: List<GoogleImagePrediction>
)

@Serializable
data class GoogleImagePrediction(
    @SerialName("bytesBase64Encoded")
    val bytesBase64Encoded: String
)

@Serializable
data class GoogleGenerateContentBody(
    val contents: List<GoogleContent>
)

@Serializable
data class GoogleContent(
    val parts: List<GooglePart>
)

@Serializable
data class GooglePart(
    val text: String? = null,
    val inlineData: GoogleInlineData? = null
)

@Serializable
data class GoogleInlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GoogleGenerateContentResponse(
    val candidates: List<GoogleCandidate>? = null
)

@Serializable
data class GoogleCandidate(
    val content: GoogleContent
)

class GoogleGenAi {
    private val http = HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(json)
        }
        engine {
            requestTimeout = 10.minutes.inWholeMilliseconds
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun image(
        prompt: String,
        model: String = "gemini-3-pro-image-preview",
        transparentBackground: Boolean = false
    ): Pair<ByteArray, String>? = runCatching {
        if (model.startsWith("gemini-")) {
            val body = GoogleGenerateContentBody(
                contents = listOf(
                    GoogleContent(
                        parts = listOf(
                            GooglePart(text = prompt + if (transparentBackground) ", blank background" else "")
                        )
                    )
                )
            )

            val response = http.post("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${secrets.google.apiKey}") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<GoogleGenerateContentResponse>()

            val part = response.candidates?.firstOrNull()?.content?.parts?.firstNotNullOfOrNull { it.inlineData }
            part?.let {
                Base64.decode(it.data) to it.mimeType
            }
        } else {
            val body = GoogleGenerateImagesBody(
                instances = listOf(GoogleImageInstance(prompt = prompt + if (transparentBackground) ", blank background" else "")),
                parameters = GoogleImageParameters(
                    sampleCount = 1,
                    outputMimeType = "image/jpeg"
                )
            )

            val response = http.post("https://generativelanguage.googleapis.com/v1beta/models/$model:generateImages?key=${secrets.google.apiKey}") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body<GoogleGenerateImagesResponse>()

            response.predictions.firstOrNull()?.bytesBase64Encoded?.let {
                Base64.decode(it) to "image/jpeg"
            }
        }
    }.onFailure {
        it.printStackTrace()
    }.getOrNull()
}
