package com.queatz.api

import com.queatz.db.Bot
import com.queatz.db.BotData
import com.queatz.db.BotDetailsBody
import com.queatz.db.botData
import com.queatz.db.bots
import com.queatz.db.groupBotData
import com.queatz.db.groupBotsOfBot
import com.queatz.parameter
import com.queatz.plugins.bots
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.botRoutes() {
    authenticate {
        get("bots") {
            respond {
                db.bots(me.id!!)
            }
        }

        post("bots") {
            respond {
                val body = call.receive<BotDetailsBody>()

                val url = body.url.trimEnd('/')

                if (!url.startsWith("https://", ignoreCase = true)) {
                    return@respond HttpStatusCode.BadRequest.description("Param 'url' must begin with \"https://\"")
                }

                val botDetails = bots.details(body.url)

                db.insert(
                    Bot(
                        url = body.url,
                        photo = body.photo,
                        creator = me.id!!,
                        name = botDetails.name,
                        description = botDetails.description,
                        keywords = botDetails.keywords,
                        config = botDetails.config
                    )
                ).also { bot ->
                    if (body.data != null) {
                        db.insert(
                            BotData(bot = bot.id!!, secret = body.data!!.secret)
                        )
                    }
                }
            }
        }

        get("bots/{id}") {
            respond {
                db.document(Bot::class, parameter("id"))?.takeIf {
                    it.isVisibleTo(me.id!!)
                } ?: HttpStatusCode.NotFound
            }
        }

        post("bots/{id}") {
            respond {
                val bot = db.document(Bot::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (bot.creator != me.id!!) {
                    return@respond HttpStatusCode.BadRequest
                }

                val update = call.receive<Bot>()

                if (update.photo != null) {
                    bot.photo = update.photo
                }

                if (update.url != null) {
                    bot.url = update.url
                }

                if (update.open != null) {
                    bot.open = update.open
                }

                db.update(bot)
            }
        }

        post("bots/{id}/reload") {
            respond {
                val bot = db.document(Bot::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (bot.creator != me.id!!) {
                    return@respond HttpStatusCode.BadRequest
                }

                bots.details(bot.url!!).apply {
                    bot.name = name
                    bot.description = description
                    bot.keywords = keywords
                    bot.config = config
                }

                db.update(bot)
            }
        }

        post("bots/{id}/delete") {
            respond {
                val bot = db.document(Bot::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (bot.creator != me.id!!) {
                    return@respond HttpStatusCode.BadRequest
                }

                // Remove all associated GroupBots
                db.groupBotsOfBot(bot.id!!).forEach { groupBot ->
                    // Remove all associated GroupBotDatas
                    db.groupBotData(groupBot.id!!)?.let { db.delete(it) }
                    db.delete(groupBot)
                }

                // Remove associated BotData
                db.botData(bot.id!!)?.let {
                    db.delete(it)
                }

                // Remove associated Bot
                db.delete(bot)

                HttpStatusCode.NoContent
            }
        }

        get("bots/{id}/data") {
            respond {
                val bot = db.document(Bot::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (bot.creator != me.id!!) {
                    return@respond HttpStatusCode.BadRequest
                }

                db.botData(bot.id!!) ?: BotData(bot = bot.id!!).let {
                    db.insert(it)
                }
            }
        }

        post("bots/{id}/data") {
            respond {
                val bot = db.document(Bot::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound

                if (bot.creator != me.id!!) {
                    return@respond HttpStatusCode.BadRequest
                }

                val botData = db.botData(parameter("id")) ?: BotData(bot = bot.id!!).let {
                    db.insert(it)
                }

                val update = call.receive<BotData>()

                if (update.secret != null) {
                    botData.secret = update.secret
                }

                db.update(botData)
            }
        }
    }
}

private fun Bot.isVisibleTo(personId: String) = creator == personId || open == true
