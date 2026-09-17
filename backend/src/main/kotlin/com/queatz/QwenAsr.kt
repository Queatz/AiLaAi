package com.queatz

import com.queatz.plugins.json
import com.queatz.plugins.secrets
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.Base64
import java.util.logging.Logger

internal const val qwenConnectTimeoutMillis = 30_000L
internal const val qwenRequestTimeoutMillis = 10 * 60 * 1_000L
internal const val qwenSocketTimeoutMillis = 10 * 60 * 1_000L
internal const val qwenRealtimeAudioChunkSize = 3_200
internal const val qwenFlashModel = "qwen-audio-3.0-asr-flash"
internal const val qwenStreamingModel = "qwen-audio-3.0-asr-flash-streaming"
internal const val qwenDefaultHost = "dashscope.aliyuncs.com"

internal val qwenClient by lazy {
    HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = qwenConnectTimeoutMillis
            requestTimeoutMillis = qwenRequestTimeoutMillis
            socketTimeoutMillis = qwenSocketTimeoutMillis
        }
        install(WebSockets)
    }
}

internal fun qwenApiHost(): String {
    return secrets.qwen?.host
        ?.removePrefix("https://")
        ?.removePrefix("http://")
        ?.trimEnd('/')
        ?.substringBefore("/")
        ?.takeIf { it.isNotBlank() }
        ?: qwenDefaultHost
}

internal fun qwenRunTaskJson(
    taskId: String,
    language: String,
): String = buildJsonObject {
    put(
        key = "header",
        element = buildJsonObject {
            put("action", "run-task")
            put("task_id", taskId)
            put("streaming", "duplex")
        }
    )
    put(
        key = "payload",
        element = buildJsonObject {
            put("task_group", "audio")
            put("task", "asr")
            put("function", "recognition")
            put("model", qwenStreamingModel)
            put(
                key = "parameters",
                element = buildJsonObject {
                    put("format", "pcm")
                    put("sample_rate", 16_000)
                    put(
                        key = "language_hints",
                        element = buildJsonArray {
                            add(JsonPrimitive(language))
                        }
                    )
                }
            )
            put(
                key = "input",
                element = buildJsonObject {}
            )
        }
    )
}.toString()

internal fun qwenFinishTaskJson(
    taskId: String,
): String = buildJsonObject {
    put(
        key = "header",
        element = buildJsonObject {
            put("action", "finish-task")
            put("task_id", taskId)
            put("streaming", "duplex")
        }
    )
    put(
        key = "payload",
        element = buildJsonObject {
            put(
                key = "input",
                element = buildJsonObject {}
            )
        }
    )
}.toString()

internal sealed class QwenStreamingEvent {
    data object TaskStarted : QwenStreamingEvent()
    data object TaskFinished : QwenStreamingEvent()
    data object Ignored : QwenStreamingEvent()
    data class TaskFailed(
        val message: String?,
    ) : QwenStreamingEvent()
    data class Transcript(
        val text: String,
        val isEnd: Boolean,
    ) : QwenStreamingEvent()
}

internal fun qwenStreamingEvent(
    text: String,
): QwenStreamingEvent {
    val root = runCatching {
        json.parseToJsonElement(text).jsonObject
    }.getOrNull() ?: return QwenStreamingEvent.Ignored

    val header = root["header"]?.jsonObject
    val event = header?.get("event")?.jsonPrimitive?.contentOrNull
        ?: return QwenStreamingEvent.Ignored

    return when (event) {
        "task-started" -> QwenStreamingEvent.TaskStarted
        "task-finished" -> QwenStreamingEvent.TaskFinished
        "task-failed" -> QwenStreamingEvent.TaskFailed(
            message = header["error_message"]?.jsonPrimitive?.contentOrNull
                ?: root["payload"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
        )
        "result-generated" -> {
            val sentence = root["payload"]?.jsonObject
                ?.get("output")?.jsonObject
                ?.get("sentence")?.jsonObject
            val heartbeat = sentence?.get("heartbeat")?.jsonPrimitive?.booleanOrNull == true
            val transcript = sentence?.get("text")?.jsonPrimitive?.contentOrNull
                ?.trim()
                .orEmpty()
            val isEnd = sentence?.get("sentence_end")?.jsonPrimitive?.booleanOrNull == true

            if (heartbeat || transcript.isBlank()) {
                QwenStreamingEvent.Ignored
            } else {
                QwenStreamingEvent.Transcript(
                    text = transcript,
                    isEnd = isEnd,
                )
            }
        }
        else -> QwenStreamingEvent.Ignored
    }
}

internal class QwenTranscriptAccumulator {
    private val completed = StringBuilder()
    private var current = ""

    fun append(
        text: String,
        isEnd: Boolean,
    ): String {
        current = text.trim()

        if (isEnd && current.isNotEmpty()) {
            if (completed.isNotEmpty()) {
                completed.append(' ')
            }
            completed.append(current)
            current = ""
        }

        return display()
    }

    fun display(): String {
        val partial = current

        return when {
            completed.isEmpty() -> partial
            partial.isEmpty() -> completed.toString()
            else -> "$completed $partial"
        }
    }
}

internal fun qwenTranscribeRequestBody(
    audioBase64: String,
    language: String,
): String = buildJsonObject {
    put("model", qwenFlashModel)
    put(
        key = "input",
        element = buildJsonObject {
            put(
                key = "messages",
                element = buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", "user")
                            put(
                                key = "content",
                                element = buildJsonArray {
                                    add(
                                        buildJsonObject {
                                            put("type", "input_audio")
                                            put(
                                                key = "input_audio",
                                                element = buildJsonObject {
                                                    put(
                                                        key = "data",
                                                        value = "data:audio/wav;base64,$audioBase64"
                                                    )
                                                }
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
    )
    put(
        key = "parameters",
        element = buildJsonObject {
            put("format", "wav")
            put("sample_rate", "16000")
            put(
                key = "language_hints",
                element = buildJsonArray {
                    add(JsonPrimitive(language))
                }
            )
        }
    )
}.toString()

internal suspend fun qwenTranscribe(
    audio: ByteArray,
    language: String,
): String? {
    val qwen = secrets.qwen ?: return null
    if (audio.isEmpty() || qwen.apiKey.isBlank()) return null

    return runCatching {
        val response = qwenClient.post("https://${qwenApiHost()}/api/v1/services/aigc/multimodal-generation/generation") {
            header("Authorization", "Bearer ${qwen.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(
                qwenTranscribeRequestBody(
                    audioBase64 = Base64.getEncoder().encodeToString(audio),
                    language = language,
                )
            )
        }
        val responseBody = response.bodyAsText()

        if (!response.status.isSuccess()) {
            Logger.getAnonymousLogger().warning(
                "[VOICE ASSISTANT] Qwen final transcription failed (status=${response.status}): $responseBody"
            )
            null
        } else {
            qwenTranscriptFromResponse(responseBody)
        }
    }.onFailure {
        it.printStackTrace()
    }.getOrNull()
}

internal fun qwenTranscriptFromResponse(
    responseBody: String,
): String? = runCatching {
    val output = json.parseToJsonElement(responseBody)
        .jsonObject["output"]
        ?.jsonObject
        ?: return@runCatching null

    output["text"]?.jsonPrimitive?.contentOrNull
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: transcriptFromChoices(output)
}.getOrNull()

private fun transcriptFromChoices(
    output: JsonObject,
): String? {
    val message = output["choices"]
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("message")
        ?.jsonObject
        ?: return null

    return when (val content = message["content"]) {
        is JsonArray -> content.firstOrNull()
            ?.jsonObject
            ?.get("text")
            ?.jsonPrimitive
            ?.contentOrNull
        is JsonPrimitive -> content.contentOrNull
        else -> null
    }?.trim()?.takeIf { it.isNotBlank() }
}

internal class PcmAudioBatcher(
    private val chunkSize: Int,
) {
    private var pending = ByteArray(0)

    fun append(
        audio: ByteArray,
    ): List<ByteArray> {
        if (audio.isEmpty()) return emptyList()

        val combined = pending + audio
        val fullChunkCount = combined.size / chunkSize
        val chunks = ArrayList<ByteArray>(fullChunkCount)

        repeat(fullChunkCount) { index ->
            val start = index * chunkSize
            chunks += combined.copyOfRange(
                fromIndex = start,
                toIndex = start + chunkSize,
            )
        }

        pending = combined.copyOfRange(
            fromIndex = fullChunkCount * chunkSize,
            toIndex = combined.size,
        )

        return chunks
    }

    fun flush(): ByteArray? = pending.takeIf { it.isNotEmpty() }?.also {
        pending = ByteArray(0)
    }
}
