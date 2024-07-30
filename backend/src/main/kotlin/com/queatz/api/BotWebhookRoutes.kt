package com.queatz.api

import com.queatz.BotAction
import com.queatz.db.groupBotByWebhook
import com.queatz.plugins.bots
import com.queatz.plugins.db
import com.queatz.plugins.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.botWebhookRoutes() {
    get("/bot/{token}") {
        respond {
            val token = call.parameters["token"]!!

            db.groupBotByWebhook(token) ?: return@respond HttpStatusCode.NotFound

            HttpStatusCode.NoContent
        }
    }

    post("/bot/{token}") {
        respond {
            val token = call.parameters["token"]!!

            val groupBot = db.groupBotByWebhook(token) ?: return@respond HttpStatusCode.NotFound

            if (groupBot.active != true) {
                return@respond HttpStatusCode.BadRequest.description("Bot is paused")
            }

            call.receive<List<BotAction>>().forEach {
                bots.action(groupBot, it)
            }

            HttpStatusCode.NoContent
        }
    }
}
