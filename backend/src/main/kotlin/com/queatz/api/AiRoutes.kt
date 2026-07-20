package com.queatz.api

import com.queatz.Ai
import com.queatz.TextPrompt
import com.queatz.db.AiJsonRequest
import com.queatz.db.AiJsonResponse
import com.queatz.db.AiPhotoRequest
import com.queatz.db.AiPhotoResponse
import com.queatz.db.AiScriptRequest
import com.queatz.db.AiSpeakRequest
import com.queatz.db.AiSpeakResponse
import com.queatz.db.AiSpeech
import com.queatz.db.AiTranscribeResponse
import com.queatz.db.Person
import com.queatz.db.PromptContext
import com.queatz.db.addPrompt
import com.queatz.db.aiSpeechByText
import com.queatz.notBlank
import com.queatz.plugins.ai
import com.queatz.plugins.db
import com.queatz.plugins.json
import com.queatz.plugins.me
import com.queatz.plugins.openAi
import com.queatz.plugins.respond
import com.queatz.plugins.secrets
import com.queatz.receiveBytes
import com.queatz.save
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.header
import io.ktor.server.routing.host
import io.ktor.server.routing.path
import io.ktor.server.routing.port
import io.ktor.server.routing.post
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.logging.Logger
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.wss as clientWss

private const val qwenConnectTimeoutMillis = 30_000L
private const val qwenRequestTimeoutMillis = 10 * 60 * 1_000L
private const val qwenSocketTimeoutMillis = 10 * 60 * 1_000L
private const val qwenRealtimeAudioChunkSize = 3_200
private const val qwenRealtimeHost = "dashscope.aliyuncs.com"

private val qwenClient by lazy {
    HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = qwenConnectTimeoutMillis
            requestTimeoutMillis = qwenRequestTimeoutMillis
            socketTimeoutMillis = qwenSocketTimeoutMillis
        }
        install(ClientWebSockets)
    }
}

fun Route.aiRoutes() {
    authenticate {
        get("/ai/styles") {
            respond {
                Ai.styles
            }
        }

        post("/ai/photo") {
            respond {
                val request = call.receive<AiPhotoRequest>()

                request.prompt.notBlank?.let {
                    db.addPrompt(me.id!!, it)
                }

                val (photoPath, dimensions) = ai.photo(
                    prefix = "group",
                    prompts = listOf(TextPrompt(request.prompt)),
                    style = request.style,
                    aspect = request.aspect ?: 1.5,
                    transparentBackground = request.removeBackground == true,
                    crop = request.crop == true
                )

                AiPhotoResponse(
                    photo = photoPath,
                    width = dimensions?.first,
                    height = dimensions?.second
                )
            }
        }

        post("/ai/speak") {
            respond {
                val request = call.receive<AiSpeakRequest>()
                val text = request.text.notBlank ?: return@respond HttpStatusCode.BadRequest

                val existing = db.aiSpeechByText(text)

                if (existing?.audio != null) {
                    AiSpeakResponse(
                        audio = existing.audio!!,
                        words = existing.words ?: emptyList()
                    )
                } else {
                    val response = openAi.speak(text)

                    if (response == null) {
                        HttpStatusCode.InternalServerError
                    } else {
                        val bytes = response.body<ByteArray>()
                        val audioPath = bytes.save("ai", "speech.ogg")
                        val words = openAi.transcribeWords(bytes)

                        val speech = db.insert(
                            AiSpeech(
                                text = text,
                                audio = audioPath,
                                words = words
                            )
                        )

                        AiSpeakResponse(
                            audio = speech.audio!!,
                            words = speech.words ?: emptyList()
                        )
                    }
                }
            }
        }

        post("/ai/script") {
            respond {
                val request = call.receive<AiScriptRequest>()

                request.prompt.notBlank?.let {
                    db.addPrompt(
                        person = me.id!!,
                        prompt = it,
                        context = PromptContext.Scripts,
                    )
                }

                val response = openAi.script(
                    prompt = request.prompt,
                    script = request.script
                )

                response ?: HttpStatusCode.InternalServerError
            }
        }

        post("/ai/transcribe") {
            respond {
                var transcribedText: String? = null

                call.receiveBytes("audio") { bytes, _ ->
                    transcribedText = openAi.transcribe(bytes)
                }

                transcribedText?.let { AiTranscribeResponse(it) } ?: HttpStatusCode.InternalServerError
            }
        }

        post("/ai/assistant/transcribe") {
            respond {
                var transcribedText: String? = null
                val language = call.request.queryParameters["language"] ?: "en"

                call.receiveBytes("audio") { bytes, _ ->
                    transcribedText = qwenTranscribe(
                        audio = bytes,
                        language = language,
                    )
                }

                transcribedText?.let { AiTranscribeResponse(it) } ?: HttpStatusCode.InternalServerError
            }
        }

        post("/ai/json") {
            respond {
                val request = call.receive<AiJsonRequest>()

                request.prompt.notBlank?.let {
                    db.addPrompt(
                        person = me.id!!,
                        prompt = it,
                        context = PromptContext.Json,
                    )
                }

                val response = openAi.json(
                    prompt = request.prompt,
                    schema = request.schema
                )

                response?.let { AiJsonResponse(it) } ?: HttpStatusCode.InternalServerError
            }
        }
    }

    webSocket("/ai/assistant") {
        val serverSession = this
        val logger = Logger.getAnonymousLogger()
        // First line the handler prints. If this NEVER appears in the backend logs while the
        // client still sees the socket close, the request is not reaching this handler at all
        // (stale/undeployed fat jar or a proxy terminating the upgrade), rather than an
        // upstream/DashScope problem.
        logger.warning("[VOICE ASSISTANT] Voice assistant: /ai/assistant handler reached")
        val token = call.request.headers[io.ktor.http.HttpHeaders.Authorization]?.removePrefix("Bearer ")
            ?: call.parameters["token"]
        val jwt = try {
            token?.let {
                com.auth0.jwt.JWT
                    .require(com.auth0.jwt.algorithms.Algorithm.HMAC256(secrets.jwt.secret))
                    .withAudience("http://0.0.0.0:8080/")
                    .withIssuer("http://0.0.0.0:8080/")
                    .build()
                    .verify(it)
            }
        } catch (e: Exception) {
            null
        }

        val personId = jwt?.getClaim("id")?.asString()
        val person = personId?.let { db.document(Person::class, it) }

        if (person == null) {
            logger.warning("[VOICE ASSISTANT] Voice assistant: closing - unauthorized (no valid token)")
            close(
                reason = CloseReason(
                    code = CloseReason.Codes.VIOLATED_POLICY,
                    message = "Unauthorized"
                )
            )
            return@webSocket
        }

        val language = call.parameters["language"] ?: "en"
        val qwenApiKey = secrets.qwen?.apiKey ?: ""
        if (qwenApiKey.isBlank()) {
            logger.warning("[VOICE ASSISTANT] Voice assistant: closing - Qwen API key is not configured (secrets.qwen.apiKey is blank)")
            close(
                reason = CloseReason(
                    code = CloseReason.Codes.CANNOT_ACCEPT,
                    message = "Missing Qwen API key"
                )
            )
            return@webSocket
        }

        // Realtime ASR uses DashScope's public realtime service, not the configured Model
        // Studio deployment host used by the final file-transcription request. The model is
        // selected only by this endpoint's `model` query parameter.
        val model = "qwen3-asr-flash-realtime"

        try {
            // Qwen-ASR-Realtime is a dedicated DashScope WebSocket API. It only accepts
            // secure connections (wss:// on port 443); the model is chosen via the `model`
            // query parameter. Flow: the server emits `session.created`, we reply with
            // `session.update` to configure transcription, stream audio as base64
            // `input_audio_buffer.append` events, read back
            // `conversation.item.input_audio_transcription.*` events, and finish with
            // `session.finish`.
            qwenClient.clientWss(
                host = qwenRealtimeHost,
                port = 443,
                path = "/api-ws/v1/realtime",
                request = {
                    header("Authorization", "Bearer $qwenApiKey")
                    parameter("model", model)
                }
            ) {
                val qwenSession = this
                // The session must be created and configured before any audio is streamed.
                // We gate audio forwarding on `session.updated` so DashScope has applied our
                // transcription config first.
                val sessionReady = CompletableDeferred<Unit>()

                // Configure the session per the Qwen-ASR-Realtime client-events spec. The app
                // records 16 kHz mono PCM16, which maps to `input_audio_format` "pcm" at
                // `sample_rate` 16000. The model is NOT set here (it is the `model` query
                // parameter); the session only takes the recognition `language`. `server_vad`
                // turn detection lets DashScope segment speech turns automatically (VAD mode).
                // Every client event must carry a unique `event_id`.
                val sessionUpdate = """
                    {
                      "event_id": "${java.util.UUID.randomUUID()}",
                      "type": "session.update",
                      "session": {
                        "input_audio_format": "pcm",
                        "sample_rate": 16000,
                        "input_audio_transcription": {
                          "language": "$language"
                        },
                        "turn_detection": {
                          "type": "server_vad"
                        }
                      }
                    }
                """.trimIndent()

                // Logged at WARN (not INFO) on purpose: a production Logback config that raises
                // the root level to WARN would silently swallow INFO, which is the most likely
                // reason previous diagnostic logging never appeared. WARN is always visible.
                logger.warning("[VOICE ASSISTANT] Voice assistant: connected to Qwen (language=$language)")

                // Forward audio from the app to Qwen, but only once the session is ready.
                val receiveJob = launch {
                    val encoder = java.util.Base64.getEncoder()
                    val audioBatcher = PcmAudioBatcher(
                        chunkSize = qwenRealtimeAudioChunkSize,
                    )
                    try {
                        sessionReady.await()
                        serverSession.incoming.consumeEach { frame ->
                            if (frame is Frame.Binary) {
                                audioBatcher.append(
                                    audio = frame.data,
                                ).forEach { audio ->
                                    qwenSession.sendRealtimeAudio(
                                        encoder = encoder,
                                        audio = audio,
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        logger.warning("[VOICE ASSISTANT] Voice assistant: error forwarding audio to Qwen")
                    } finally {
                        // In VAD mode DashScope segments turns automatically, so end the
                        // session with `session.finish` (input_audio_buffer.commit is
                        // Manual-mode only). DashScope then flushes the final transcription
                        // and replies with `session.finished`.
                        runCatching {
                            audioBatcher.flush()?.let { audio ->
                                qwenSession.sendRealtimeAudio(
                                    encoder = encoder,
                                    audio = audio,
                                )
                            }
                            qwenSession.send(
                                Frame.Text("""{"event_id":"${java.util.UUID.randomUUID()}","type":"session.finish"}""")
                            )
                        }
                    }
                }

                // Forward transcription results from Qwen back to the app.
                val sendJob = launch {
                    try {
                        qwenSession.incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                val event = runCatching { json.parseToJsonElement(text) }
                                    .getOrNull()?.jsonObject
                                val eventType = event?.get("type")?.jsonPrimitive?.content
                                logger.fine("[VOICE ASSISTANT] Voice assistant: Qwen event type=$eventType")

                                when (eventType) {
                                    "session.created" -> {
                                        logger.warning("[VOICE ASSISTANT] Voice assistant: Qwen session created")
                                        qwenSession.send(Frame.Text(sessionUpdate))
                                    }
                                    "session.updated" -> {
                                        logger.warning("[VOICE ASSISTANT] Voice assistant: Qwen session updated")
                                        if (!sessionReady.isCompleted) {
                                            sessionReady.complete(Unit)
                                        }
                                    }
                                    "conversation.item.input_audio_transcription.text",
                                    "conversation.item.input_audio_transcription.completed" -> {
                                        // `.text` carries incremental results, `.completed`
                                        // the final transcript. Both expose the recognized
                                        // text via `transcript` (or `text` on some events).
                                        val transcript = event["transcript"]?.jsonPrimitive?.content
                                            ?: event["text"]?.jsonPrimitive?.content
                                        if (!transcript.isNullOrBlank()) {
                                            serverSession.send(Frame.Text(transcript))
                                        }
                                    }
                                    "conversation.item.input_audio_transcription.failed" -> {
                                        val errorMessage = event["error"]?.jsonObject
                                            ?.get("message")?.jsonPrimitive?.content
                                        logger.warning("[VOICE ASSISTANT] Voice assistant: Qwen transcription failed: $errorMessage")
                                    }
                                    "session.finished" -> {
                                        logger.warning("[VOICE ASSISTANT] Voice assistant: Qwen session finished")
                                    }
                                    "error" -> {
                                        val errorMessage = event["error"]?.jsonObject
                                            ?.get("message")?.jsonPrimitive?.content
                                            ?: event["message"]?.jsonPrimitive?.content
                                        logger.warning("[VOICE ASSISTANT] Voice assistant: Qwen error: $errorMessage")
                                        if (!sessionReady.isCompleted) {
                                            sessionReady.completeExceptionally(
                                                Exception("Qwen error: $errorMessage")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        logger.warning("[VOICE ASSISTANT] Voice assistant: error receiving results from Qwen")
                    } finally {
                        if (!sessionReady.isCompleted) {
                            sessionReady.completeExceptionally(
                                Exception("Qwen session closed before it was ready")
                            )
                        }
                    }
                }

                joinAll(receiveJob, sendJob)

                // Surface why Qwen ended the session so a rejected handshake/config is
                // diagnosable next time.
                val qwenCloseReason = runCatching { qwenSession.closeReason.await() }.getOrNull()
                logger.warning(
                    "[VOICE ASSISTANT] Voice assistant: Qwen session closed (code=${qwenCloseReason?.code}, reason=${qwenCloseReason?.message})"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // The WebSocket handshake failed (e.g. HTTP 403). Ktor's WebSocketException only
            // reports the status code ("expected 101 but was 403") and throws away the
            // response body, so we never see *why* Qwen rejected us. Re-issue the exact same
            // URL as a plain HTTP request to capture the status and body Qwen returns and log
            // it, so the real reason (bad key, model availability, or invalid configuration)
            // is visible in the logs.
            val qwenReason = runCatching {
                val response = qwenClient.request("https://$qwenRealtimeHost/api-ws/v1/realtime") {
                    method = HttpMethod.Get
                    parameter("model", model)
                    header("Authorization", "Bearer $qwenApiKey")
                }
                "status=${response.status}, body=${response.bodyAsText()}"
            }.getOrElse { "unavailable (${it.localizedMessage})" }
            logger.warning(
                "[VOICE ASSISTANT] Voice assistant: connection to Qwen failed (host=$qwenRealtimeHost, model=$model): ${e.localizedMessage}; Qwen response: $qwenReason"
            )
            close(
                reason = CloseReason(
                    code = CloseReason.Codes.INTERNAL_ERROR,
                    message = e.localizedMessage ?: "Unknown error"
                )
            )
        }
    }
}

private suspend fun qwenTranscribe(
    audio: ByteArray,
    language: String,
): String? {
    val qwen = secrets.qwen ?: return null
    if (audio.isEmpty()) return null

    return runCatching {
        val response = qwenClient.post("https://${qwen.host}/api/v1/services/aigc/multimodal-generation/generation") {
            header("Authorization", "Bearer ${qwen.apiKey}")
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    buildJsonObject {
                        put("model", "qwen3-asr-flash")
                        put(
                            "input",
                            buildJsonObject {
                                put(
                                    "messages",
                                    buildJsonArray {
                                        add(
                                            buildJsonObject {
                                                put("role", "user")
                                                put(
                                                    "content",
                                                    buildJsonArray {
                                                        add(
                                                            buildJsonObject {
                                                                put(
                                                                    "audio",
                                                                    "data:audio/wav;base64," +
                                                                        java.util.Base64.getEncoder().encodeToString(audio)
                                                                )
                                                            }
                                                        )
                                                        add(
                                                            buildJsonObject {
                                                                put("text", "Transcribe this audio in $language.")
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
                    }.toString()
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

private suspend fun WebSocketSession.sendRealtimeAudio(
    encoder: java.util.Base64.Encoder,
    audio: ByteArray,
) {
    val audioBase64 = encoder.encodeToString(audio)

    send(
        Frame.Text(
            """{"event_id":"${java.util.UUID.randomUUID()}","type":"input_audio_buffer.append","audio":"$audioBase64"}"""
        )
    )
}

internal fun qwenTranscriptFromResponse(
    responseBody: String,
): String? = runCatching {
    json.parseToJsonElement(responseBody)
        .jsonObject["output"]
        ?.jsonObject
        ?.get("choices")
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("message")
        ?.jsonObject
        ?.get("content")
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("text")
        ?.jsonPrimitive
        ?.content
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}.getOrNull()
