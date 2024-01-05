package com.queatz.api

import com.queatz.Ai
import com.queatz.TextPrompt
import com.queatz.db.AiPhotoRequest
import com.queatz.db.AiPhotoResponse
import com.queatz.plugins.ai
import com.queatz.plugins.respond
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
                        request.style
                    )
                )
            }
        }
    }
}
