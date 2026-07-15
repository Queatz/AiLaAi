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
import com.queatz.db.aiSpeechByText
import com.queatz.db.PromptContext
import com.queatz.db.addPrompt
import com.queatz.notBlank
import com.queatz.save
import com.queatz.plugins.ai
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.openAi
import com.queatz.plugins.respond
import com.queatz.receiveBytes
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.application.log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.wss as clientWss
import io.ktor.client.request.header
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.channels.consumeEach
import com.queatz.plugins.secrets
import com.queatz.plugins.json
import com.queatz.db.Person
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
        val logger = call.application.log
        // First line the handler prints. If this NEVER appears in the backend logs while the
        // client still sees the socket close, the request is not reaching this handler at all
        // (stale/undeployed fat jar or a proxy terminating the upgrade), rather than an
        // upstream/DashScope problem.
        logger.warn("Voice assistant: /ai/assistant handler reached")
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
            logger.warn("Voice assistant: closing - unauthorized (no valid token)")
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
            logger.warn("Voice assistant: closing - Qwen API key is not configured (secrets.qwen.apiKey is blank)")
            close(
                reason = CloseReason(
                    code = CloseReason.Codes.CANNOT_ACCEPT,
                    message = "Missing Qwen API key"
                )
            )
            return@webSocket
        }

        try {
            // DashScope only accepts secure WebSocket connections (wss:// on port 443).
            // Using the plain ws:// host overload previously caused the handshake to fail
            // and the session to close with INTERNAL_ERROR.
            qwenClient.clientWss(
                host = "dashscope.aliyuncs.com",
                port = 443,
                path = "/api-ws/v1/inference/",
                request = {
                    header("Authorization", "Bearer $qwenApiKey")
                    header("X-DashScope-DataInspection", "enable")
                }
            ) {
                val qwenSession = this
                val taskId = java.util.UUID.randomUUID().toString().replace("-", "")
                // DashScope requires waiting for the `task-started` event before streaming audio.
                val taskStarted = CompletableDeferred<Unit>()
                // DashScope's duplex `run-task` message must wrap everything in a `payload`
                // object (task_group/task/function/model/input/parameters). Sending the
                // parameters at the top level (as the previous version did) makes DashScope
                // reject the task and close the socket with a normal close code before any
                // audio is transcribed - which is exactly the "closes with NORMAL, no
                // transcription" symptom we were seeing.
                // qwen3-asr-flash automatically detects the spoken language, so we intentionally
                // do not send a language hint: the model supports a limited set of languages and
                // an unsupported hint would fail the task instead of falling back to detection.
                val startMessage = """
                    {
                      "header": {
                        "action": "run-task",
                        "task_id": "$taskId",
                        "streaming": "duplex"
                      },
                      "payload": {
                        "task_group": "audio",
                        "task": "asr",
                        "function": "recognition",
                        "model": "qwen3-asr-flash-realtime",
                        "input": {},
                        "parameters": {
                          "format": "pcm",
                          "sample_rate": 16000
                        }
                      }
                    }
                """.trimIndent()
                // Logged at WARN (not INFO) on purpose: a production Logback config that raises
                // the root level to WARN would silently swallow INFO, which is the most likely
                // reason previous diagnostic logging never appeared. WARN is always visible.
                logger.warn("Voice assistant: connected to Qwen, starting task $taskId (language=$language)")
                logger.warn("Voice assistant: run-task request = $startMessage")
                qwenSession.send(Frame.Text(startMessage))

                // Forward audio from the app to Qwen, but only once the task has started.
                val receiveJob = launch {
                    try {
                        taskStarted.await()
                        serverSession.incoming.consumeEach { frame ->
                            if (frame is Frame.Binary) {
                                qwenSession.send(Frame.Binary(fin = true, data = frame.data))
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Voice assistant: error forwarding audio to Qwen", e)
                    } finally {
                        val finishMessage = """
                            {
                              "header": {
                                "action": "finish-task",
                                "task_id": "$taskId"
                              }
                            }
                        """.trimIndent()
                        runCatching {
                            qwenSession.send(Frame.Text(finishMessage))
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
                                // response (including the exact `task-failed` error message) is
                                // always visible. This is what finally makes the "closes with
                                // NORMAL, no transcription, no logs" case diagnosable instead of
                                // guessing at the protocol.
                                logger.warn("Voice assistant: Qwen frame = $text")
                                val jsonElement = runCatching { json.parseToJsonElement(text) }.getOrNull()
                                val header = jsonElement?.jsonObject?.get("header")?.jsonObject

                                when (header?.get("event")?.jsonPrimitive?.content) {
                                    "task-started" -> {
                                        logger.warn("Voice assistant: Qwen task started")
                                        taskStarted.complete(Unit)
                                    }
                                    "task-failed" -> {
                                        val errorMessage = header["error_message"]?.jsonPrimitive?.content
                                        logger.error("Voice assistant: Qwen task failed: $errorMessage")
                                        if (!taskStarted.isCompleted) {
                                            taskStarted.completeExceptionally(
                                                Exception("Qwen task failed: $errorMessage")
                                            )
                                        }
                                    }
                                }

                                val transcript = jsonElement?.jsonObject?.get("payload")
                                    ?.jsonObject?.get("output")
                                    ?.jsonObject?.get("sentence")
                                    ?.jsonObject?.get("text")
                                    ?.jsonPrimitive?.content
                                if (transcript != null) {
                                    serverSession.send(Frame.Text(transcript))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Voice assistant: error receiving results from Qwen", e)
                    } finally {
                        if (!taskStarted.isCompleted) {
                            taskStarted.completeExceptionally(
                                Exception("Qwen session closed before task started")
                            )
                        }
                    }
                }

                joinAll(receiveJob, sendJob)

                // Surface why Qwen ended the session so a rejected task/handshake is
                // diagnosable next time (previously nothing was logged for this case).
                val qwenCloseReason = runCatching { qwenSession.closeReason.await() }.getOrNull()
                logger.warn(
                    "Voice assistant: Qwen session closed (code=${qwenCloseReason?.code}, reason=${qwenCloseReason?.message})"
                )
            }
        } catch (e: Exception) {
            logger.error("Voice assistant: connection to Qwen failed", e)
            close(
                reason = CloseReason(
                    code = CloseReason.Codes.INTERNAL_ERROR,
                    message = e.localizedMessage ?: "Unknown error"
                )
            )
        }
    }
}
