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
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocket as clientWebSocket
import io.ktor.http.HttpMethod
import io.ktor.client.request.header
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

        webSocket("/ai/assistant") {
            val serverSession = this
            val person = call.principal<JWTPrincipal>()
                ?.getClaim("id", String::class)
                ?.let { db.document(Person::class, it) }
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))

            val language = call.parameters["language"] ?: "en"
            val qwenApiKey = secrets.qwen?.apiKey ?: ""
            if (qwenApiKey.isBlank()) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing Qwen API key"))
                return@webSocket
            }

            try {
                qwenClient.clientWebSocket(
                    method = HttpMethod.Get,
                    host = "dashscope.aliyuncs.com",
                    path = "/api-ws/v1/inference/",
                    request = {
                        header("Authorization", "Bearer $qwenApiKey")
                    }
                ) {
                    val qwenSession = this
                    val taskId = java.util.UUID.randomUUID().toString().replace("-", "")
                    val startMessage = """
                        {
                          "header": {
                            "action": "run-task",
                            "task_id": "$taskId",
                            "streaming": "duplex"
                          },
                          "parameters": {
                            "model": "qwen3-asr-flash-realtime",
                            "format": "pcm",
                            "sample_rate": 16000,
                            "language": "$language"
                          }
                        }
                    """.trimIndent()
                    qwenSession.send(Frame.Text(startMessage))

                    val receiveJob = launch {
                        try {
                            serverSession.incoming.consumeEach { frame ->
                                if (frame is Frame.Binary) {
                                    qwenSession.send(Frame.Binary(fin = true, data = frame.data))
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
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

                    val sendJob = launch {
                        try {
                            qwenSession.incoming.consumeEach { frame ->
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    val jsonElement = runCatching { json.parseToJsonElement(text) }.getOrNull()
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
                            e.printStackTrace()
                        }
                    }

                    joinAll(receiveJob, sendJob)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, e.localizedMessage ?: "Unknown error"))
            }
        }
    }
}
