package com.queatz.api

import com.queatz.Ai
import com.queatz.PcmAudioBatcher
import com.queatz.QwenStreamingEvent
import com.queatz.QwenTranscriptAccumulator
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
import com.queatz.plugins.me
import com.queatz.plugins.openAi
import com.queatz.plugins.respond
import com.queatz.plugins.secrets
import com.queatz.qwenApiHost
import com.queatz.qwenClient
import com.queatz.qwenFinishTaskJson
import com.queatz.qwenRealtimeAudioChunkSize
import com.queatz.qwenRunTaskJson
import com.queatz.qwenStreamingEvent
import com.queatz.qwenStreamingModel
import com.queatz.qwenTranscribe
import com.queatz.receiveBytes
import com.queatz.save
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
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
import kotlinx.coroutines.withTimeout
import java.util.UUID
import java.util.logging.Logger
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.plugins.websocket.wss as clientWss

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

        val host = qwenApiHost()
        val taskId = UUID.randomUUID().toString()

        try {
            qwenClient.clientWss(
                host = host,
                port = 443,
                path = "/api-ws/v1/inference",
                request = {
                    header("Authorization", "Bearer $qwenApiKey")
                }
            ) {
                val qwenSession = this
                val sessionReady = CompletableDeferred<Unit>()
                val accumulator = QwenTranscriptAccumulator()

                logger.warning("[VOICE ASSISTANT] Voice assistant: connected to Qwen (host=$host, model=$qwenStreamingModel, language=$language)")

                qwenSession.send(
                    Frame.Text(
                        qwenRunTaskJson(
                            taskId = taskId,
                            language = language,
                        )
                    )
                )

                val receiveJob = launch {
                    val audioBatcher = PcmAudioBatcher(
                        chunkSize = qwenRealtimeAudioChunkSize,
                    )
                    try {
                        withTimeout(15.seconds) {
                            sessionReady.await()
                        }
                        serverSession.incoming.consumeEach { frame ->
                            if (frame is Frame.Binary) {
                                audioBatcher.append(
                                    audio = frame.data,
                                ).forEach { audio ->
                                    qwenSession.send(Frame.Binary(fin = true, data = audio))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        logger.warning("[VOICE ASSISTANT] Voice assistant: error forwarding audio to Qwen")
                    } finally {
                        runCatching {
                            audioBatcher.flush()?.let { audio ->
                                qwenSession.send(Frame.Binary(fin = true, data = audio))
                            }
                            qwenSession.send(
                                Frame.Text(
                                    qwenFinishTaskJson(
                                        taskId = taskId,
                                    )
                                )
                            )
                        }
                    }
                }

                val sendJob = launch {
                    try {
                        qwenSession.incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                when (
                                    val event = qwenStreamingEvent(
                                        text = frame.readText(),
                                    )
                                ) {
                                    QwenStreamingEvent.TaskStarted -> {
                                        logger.warning("[VOICE ASSISTANT] Voice assistant: Qwen task started")
                                        if (!sessionReady.isCompleted) {
                                            sessionReady.complete(Unit)
                                        }
                                    }
                                    is QwenStreamingEvent.Transcript -> {
                                        val transcript = accumulator.append(
                                            text = event.text,
                                            isEnd = event.isEnd,
                                        )
                                        if (transcript.isNotBlank()) {
                                            serverSession.send(Frame.Text(transcript))
                                        }
                                    }
                                    is QwenStreamingEvent.TaskFailed -> {
                                        logger.warning("[VOICE ASSISTANT] Voice assistant: Qwen task failed: ${event.message}")
                                        if (!sessionReady.isCompleted) {
                                            sessionReady.completeExceptionally(
                                                Exception("Qwen error: ${event.message}")
                                            )
                                        }
                                        qwenSession.close()
                                    }
                                    QwenStreamingEvent.TaskFinished -> {
                                        logger.warning("[VOICE ASSISTANT] Voice assistant: Qwen task finished")
                                        qwenSession.close()
                                    }
                                    QwenStreamingEvent.Ignored -> Unit
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
                        receiveJob.cancel()
                    }
                }

                joinAll(receiveJob, sendJob)

                val qwenCloseReason = runCatching { qwenSession.closeReason.await() }.getOrNull()
                logger.warning(
                    "[VOICE ASSISTANT] Voice assistant: Qwen session closed (code=${qwenCloseReason?.code}, reason=${qwenCloseReason?.message})"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logger.warning(
                "[VOICE ASSISTANT] Voice assistant: connection to Qwen failed (host=$host, model=$qwenStreamingModel): ${e.localizedMessage}"
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
