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
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
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
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.logging.Logger
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.wss as clientWss

private val qwenClient = HttpClient(CIO) {
    install(ClientWebSockets)
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

        // Qwen-ASR-Realtime selects the model ONLY via the `model` query parameter on the
        // realtime endpoint. `qwen3-asr-flash-realtime` is a valid model name, so if
        // DashScope answers the handshake with `400 "Model not exist"`, the model simply is
        // not published/enabled for the DashScope region that `secrets.qwen.host` targets
        // (China-Beijing `dashscope.aliyuncs.com` vs Singapore `dashscope-intl.aliyuncs.com`).
        // In that case point `secrets.qwen.host` at the region where the account has the
        // model enabled - it is not a bug in this handler.
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
                host = secrets.qwen!!.host,
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
                    try {
                        sessionReady.await()
                        val encoder = java.util.Base64.getEncoder()
                        serverSession.incoming.consumeEach { frame ->
                            if (frame is Frame.Binary) {
                                // Audio is sent as base64 inside an `input_audio_buffer.append`
                                // event (each event needs a unique `event_id`).
                                val audioBase64 = encoder.encodeToString(frame.data)
                                qwenSession.send(
                                    Frame.Text(
                                        """{"event_id":"${java.util.UUID.randomUUID()}","type":"input_audio_buffer.append","audio":"$audioBase64"}"""
                                    )
                                )
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
                                // Log every raw frame from DashScope so the upstream's actual
                                // response (including the exact `error` message) is always
                                // visible when diagnosing issues.
                                logger.warning("[VOICE ASSISTANT] Voice assistant: Qwen frame = $text")
                                val event = runCatching { json.parseToJsonElement(text) }
                                    .getOrNull()?.jsonObject

                                when (event?.get("type")?.jsonPrimitive?.content) {
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
            // it, so the real reason (bad key, model not enabled for the account, wrong
            // region/host, etc.) is visible in the logs.
            val qwenReason = runCatching {
                val response = qwenClient.request("https://${secrets.qwen!!.host}/api-ws/v1/realtime") {
                    method = HttpMethod.Get
                    parameter("model", model)
                    header("Authorization", "Bearer $qwenApiKey")
                }
                "status=${response.status}, body=${response.bodyAsText()}"
            }.getOrElse { "unavailable (${it.localizedMessage})" }
            // Include the host + model so a `400 "Model not exist"` (valid model name but not
            // enabled for this region) is immediately attributable to the wrong region host.
            logger.warning(
                "[VOICE ASSISTANT] Voice assistant: connection to Qwen failed (host=${secrets.qwen?.host}, model=$model): ${e.localizedMessage}; Qwen response: $qwenReason"
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
