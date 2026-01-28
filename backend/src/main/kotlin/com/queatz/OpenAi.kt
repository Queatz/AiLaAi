package com.queatz

import app.ailaai.shared.resources.ScriptsResources
import com.fasterxml.jackson.annotation.JsonAlias
import com.queatz.db.AiScriptResponse
import com.queatz.plugins.json
import com.queatz.plugins.secrets
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import java.util.logging.Logger.getAnonymousLogger
import kotlin.text.Charsets.UTF_8
import kotlin.time.Duration.Companion.minutes
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class OpenAiSpeakBody(
    val input: String,
    val model: String = "tts-1-hd",
    val voice: String = "shimmer",
    @SerialName("response_format") val format: String = "opus",
    val speed: Double = 1.0,
)

@Serializable
data class OpenAiCompletionsMessage(
    val role: String = "user",
    val content: String,
)

@Serializable
data class OpenAiResponsesBody(
    val model: String = "o4-mini",
    val input: List<OpenAiCompletionsMessage>,
    val text: JsonObject,
)

@Serializable
data class OpenAiResponsesResponse(
    val output: List<OpenAiOutput>
)

@Serializable
data class OpenAiOutput(
    val type: String,
    val status: String? = null,
    val role: String? = null,
    @JsonAlias("revised_prompt")
    val revisedPrompt: String? = null,
    val result: String? = null,
    val content: List<OpenAiOutputFormat> = emptyList(),
)

@Serializable
data class OpenAiOutputFormat(
    val type: String,
    val text: String
)

@Serializable
data class OpenAiStructuredCodeOutput(
    val description: String,
    val code: String,
)

@Serializable
data class OpenAiTranscriptionResponse(
    val text: String
)

@Serializable
data class OpenAiTool(
    val type: String,
    val background: String? = null,
    val quality: String = "high"
)

@Serializable
data class OpenAiImageGenerationBody(
    val model: String = "gpt-5-mini",
    val input: List<OpenAiCompletionsMessage>,
    val tools: List<OpenAiTool>
)

private val SCRIPT_SYSTEM_PROMPT = """
    You are helping someone write a Kotlin Script that run inside a JVM server.
    Because this is a Kotlin Script, the `return` statement cannot be used at the root level. 

    Your code will be inserted into a code editor.

    - You document the code that you create so that everyone can understand what the code does.
    - You prefer Kotlin imports over Java ones (ex. kotlinx.datetime)

    IMPORTANT:

     - You *always* return the *entire* script. Your code will be run and must compile without issues.
     - You triple check that all code is valid Kotlin, and all necessary imports exist.
     - The user will provide you with documentation related their current scripting context.
""".trimIndent()

private val SCRIPT_DOCUMENTATION_PROMPT = """
    Here is the documentation related the my current scripting context:

    ${ScriptsResources.documentation}
""".trimIndent()

private val SCRIPT_CURRENT_PROMPT = """
    Here is the current script:

""".trimIndent()

private val SCRIPT_DEFAULT_PROMPT = """
    Please check this script for errors, and resolve any comments or todos in the code.
""".trimIndent()

private val JSON_SCHEMA by lazy {
    """
    {
      "format": {
        "type": "json_schema",
        "name": "script",
        "description": "Kotlin Script (kts)",
        "strict": true,
        "schema": {
          "type": "object",
          "properties": {
            "description": {
              "type": "string"
            },
            "code": {
              "type": "string"
            }
          },
          "required": [
            "description",
            "code"
          ],
          "additionalProperties": false
        }
      }
    }
    """.let { json.decodeFromString<JsonObject>(it) }
}

class OpenAi {

    val http = HttpClient(CIO) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }

        engine {
            requestTimeout = 5.minutes.inWholeMilliseconds
        }
    }

    suspend fun script(
        prompt: String,
        script: String? = null,
    ): AiScriptResponse? {
        return runCatching {
            http.post("https://api.openai.com/v1/responses") {
                bearerAuth(secrets.openAi.key)
                contentType(ContentType.Application.Json.withCharset(UTF_8))
                setBody(
                    OpenAiResponsesBody(
                        input = listOfNotNull(
                            OpenAiCompletionsMessage(
                                role = "system",
                                content = SCRIPT_SYSTEM_PROMPT
                            ),
                            OpenAiCompletionsMessage(
                                role = "user",
                                content = SCRIPT_DOCUMENTATION_PROMPT
                            ),
                            OpenAiCompletionsMessage(
                                role = "user",
                                content = SCRIPT_CURRENT_PROMPT
                            ).takeIf {
                                !script.isNullOrBlank()
                            },
                            OpenAiCompletionsMessage(
                                role = "user",
                                content = script.orEmpty()
                            ).takeIf {
                                !script.isNullOrBlank()
                            },
                            OpenAiCompletionsMessage(
                                role = "user",
                                content = prompt.ifBlank { SCRIPT_DEFAULT_PROMPT }
                            ),
                        ),
                        text = JSON_SCHEMA
                    )
                )
            }.also {
                getAnonymousLogger().warning("OpenAi response: ${it.bodyAsText()}")
            }.body<OpenAiResponsesResponse>()
        }.getOrNull()?.let { response ->
            response.output
                .firstOrNull { it.type == "message" }
                ?.content
                ?.firstOrNull { it.type == "output_text" }
                ?.text?.let {
                val response = json.decodeFromString<OpenAiStructuredCodeOutput>(it)

                """
                |/**
                | * ${response.description.lines().joinToString("\n * ") { it }}
                | */
                | 
                |${response.code}
                """.trimMargin()
            }
        }?.let { AiScriptResponse(it) }
    }

    suspend fun speak(text: String): HttpResponse? = runCatching {
        http.post("https://api.openai.com/v1/audio/speech") {
            bearerAuth(secrets.openAi.key)
            contentType(ContentType.Application.Json.withCharset(UTF_8))
            setBody(
                OpenAiSpeakBody(input = text)
            )
        }
    }.onFailure {
        it.printStackTrace()
    }.getOrNull()

    suspend fun transcribe(audioBytes: ByteArray): String? = runCatching {
        http.post("https://api.openai.com/v1/audio/transcriptions") {
            bearerAuth(secrets.openAi.key)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = audioBytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, "audio/webm")
                                append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"audio.webm\"")
                            }
                        )
                        append(
                            key = "model",
                            value = "whisper-1"
                        )
                    }
                )
            )
        }.body<OpenAiTranscriptionResponse>().text
    }.onFailure {
        it.printStackTrace()
    }.getOrNull()

    suspend fun json(
        prompt: String,
        schema: JsonObject
    ): String? {
        return runCatching {
            http.post("https://api.openai.com/v1/responses") {
                bearerAuth(secrets.openAi.key)
                contentType(ContentType.Application.Json.withCharset(UTF_8))
                setBody(
                    OpenAiResponsesBody(
                        input = listOf(
                            OpenAiCompletionsMessage(
                                role = "user",
                                content = prompt
                            )
                        ),
                        text = schema
                    )
                )
            }.also {
                getAnonymousLogger().warning("OpenAi JSON response: ${it.bodyAsText()}")
            }.body<OpenAiResponsesResponse>()
        }.getOrNull()?.let { response ->
            response.output
                .firstOrNull { it.type == "message" }
                ?.content
                ?.firstOrNull { it.type == "output_text" }
                ?.text
        }
    }

    // Generate image using OpenAI Responses API
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun image(
        prompt: String,
        transparentBackground: Boolean = false
    ): ByteArray? = runCatching {
        val tool = OpenAiTool(
            type = "image_generation",
            background = if (transparentBackground) "transparent" else null
        )
        val body = OpenAiImageGenerationBody(
            input = listOf(OpenAiCompletionsMessage(role = "user", content = prompt)),
            tools = listOf(tool)
        )
        http.post("https://api.openai.com/v1/responses") {
            bearerAuth(secrets.openAi.key)
            contentType(ContentType.Application.Json.withCharset(UTF_8))
            setBody(body)
        }.also {
            getAnonymousLogger().info("OpenAI image response: ${it.bodyAsText()}")
        }.body<OpenAiResponsesResponse>()
            .output
            .firstOrNull { it.result != null }
            ?.result
            ?.let { image ->
                Base64.decode(image)
            } ?: throw IllegalStateException("No image URL returned from OpenAI")
    }.onFailure {
        it.printStackTrace()
    }.getOrNull()
}
