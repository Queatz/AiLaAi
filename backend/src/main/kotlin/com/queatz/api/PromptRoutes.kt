package com.queatz.api

import com.queatz.db.Prompt
import com.queatz.db.PromptContext
import com.queatz.db.addPrompt
import com.queatz.db.prompts
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.promptRoutes() {
    authenticate {
        get("/prompts") {
            respond {
                db.prompts(
                    person = me.id!!,
                    context = call.parameters["context"]?.let { PromptContext.valueOf(it) },
                    offset = call.parameters["offset"]?.toInt() ?: 0,
                    limit = call.parameters["limit"]?.toInt() ?: 20
                )
            }
        }
        post("/prompts") {
            respond {
                val prompt = call.receive<Prompt>()

                db.addPrompt(
                    person = me.id!!,
                    prompt = prompt.prompt!!,
                    context = prompt.context
                ) ?: HttpStatusCode.BadRequest
            }
        }
    }
}
