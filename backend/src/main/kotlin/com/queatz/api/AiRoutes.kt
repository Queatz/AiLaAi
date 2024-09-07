package com.queatz.api

import com.queatz.Ai
import com.queatz.OpenAiSpeakBody
import com.queatz.TextPrompt
import com.queatz.db.AiPhotoRequest
import com.queatz.db.AiPhotoResponse
import com.queatz.db.AiSpeakRequest
import com.queatz.plugins.ai
import com.queatz.plugins.openAi
import com.queatz.plugins.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
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

                AiPhotoResponse(
                    ai.photo(
                        "group",
                        buildList {
                            add(
                                TextPrompt(request.prompt)
                            )
                        },
                        request.style,
                        aspect = request.aspect ?: 1.5,
                        transparentBackground = request.removeBackground ?: false
                    )
                )
            }
        }
        
        post("/ai/speak") {
            respond {
                val request = call.receive<AiSpeakRequest>()
                openAi.speak(request.text) ?: HttpStatusCode.InternalServerError
            }
        }
    }
}
