package com.queatz.api

import com.queatz.Ai
import com.queatz.TextPrompt
import com.queatz.db.AiJsonRequest
import com.queatz.db.AiJsonResponse
import com.queatz.db.AiPhotoRequest
import com.queatz.db.AiPhotoResponse
import com.queatz.db.AiScriptRequest
import com.queatz.db.AiSpeakRequest
import com.queatz.db.AiTranscribeResponse
import com.queatz.db.PromptContext
import com.queatz.db.addPrompt
import com.queatz.notBlank
import com.queatz.plugins.ai
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.openAi
import com.queatz.plugins.respond
import com.queatz.receiveBytes
import io.ktor.client.call.body
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.*

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
            val request = call.receive<AiSpeakRequest>()
            val response = openAi.speak(request.text)

            if (response == null) {
                respond { HttpStatusCode.InternalServerError }
                return@post
            } else {
                call.respondBytes(response.body<ByteArray>(), ContentType.Audio.OGG)
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
}
