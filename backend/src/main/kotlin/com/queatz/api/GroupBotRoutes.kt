package com.queatz.api

import com.queatz.ReinstallBotBody
import com.queatz.db.Bot
import com.queatz.db.GroupBot
import com.queatz.db.groupBotData
import com.queatz.db.member
import com.queatz.parameter
import com.queatz.plugins.bots
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.groupBotRoutes() {
    authenticate {
        get("/group-bots/{id}") {
            respond {
                val groupBot = db.document(GroupBot::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (db.member(me.id!!, groupBot.group!!)?.host != true) {
                    return@respond HttpStatusCode.BadRequest
                }

                groupBot
            }
        }

        post("/group-bots/{id}") {
            respond {
                val groupBot = db.document(GroupBot::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (db.member(me.id!!, groupBot.group!!)?.host != true) {
                    return@respond HttpStatusCode.BadRequest
                }

                val update = call.receive<GroupBot>()

                val bot = db.document(Bot::class, groupBot.bot!!)
                    ?: return@respond HttpStatusCode.BadRequest.description("Bot doesn't exist")

                val groupBotData = db.groupBotData(groupBot.id!!)
                    ?: return@respond HttpStatusCode.BadRequest.description("Group bot data doesn't exist")

                if (update.active != null) {
                    groupBot.active = update.active
                }

                if (update.config != null) {
                    groupBot.config = update.config

                    bots.reinstall(
                        url = bot.url!!,
                        authToken = groupBotData.authToken!!,
                        body = ReinstallBotBody(groupBot.config)
                    )
                }

                db.update(groupBot)
            }
        }

        post("/group-bots/{id}/delete") {
            respond {
                val groupBot = db.document(GroupBot::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (db.member(me.id!!, groupBot.group!!)?.host != true) {
                    return@respond HttpStatusCode.BadRequest
                }

                db.delete(groupBot)

                runCatching {
                    val bot = db.document(Bot::class, groupBot.bot!!)
                        ?: throw Exception("Bot doesn't exist")

                    val groupBotData = db.groupBotData(groupBot.id!!)
                        ?: throw Exception("Group bot data doesn't exist")

                    bots.uninstall(
                        url = bot.url!!,
                        authToken = groupBotData.authToken!!
                    )
                }.onFailure {
                    it.printStackTrace()
                }

                HttpStatusCode.NoContent
            }
        }
    }
}
