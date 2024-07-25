package com.queatz.api

import com.queatz.db.Bot
import com.queatz.db.CreateBotBody
import com.queatz.db.bots
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

fun Route.botRoutes() {
    authenticate {
        get("bots") {
            respond {
                db.bots(me.id!!)
            }
        }

        post("bots") {
            respond {
                val body = call.receive<CreateBotBody>()
                val botDetails = bots.details(body.url)

                db.insert(
                    Bot(
                        url = body.url,
                        creator = me.id!!,
                        name = botDetails.name,
                        description = botDetails.description,
                        keywords = botDetails.keywords,
                        config = botDetails.config
                    )
                )
            }
        }

        get("bots/{id}") {
            respond {
                db.document(Bot::class, parameter("id")) ?: HttpStatusCode.NotFound
            }
        }

        post("bots/{id}") {
            respond {
                val bot = db.document(Bot::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound

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

                db.update(bot)
            }
        }

        post("bots/{id}/reload") {
            respond {
                val bot = db.document(Bot::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound

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
                val bot = db.document(Bot::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound

                if (bot.creator != me.id!!) {
                    return@respond HttpStatusCode.BadRequest
                }

                db.delete(bot)

                HttpStatusCode.NoContent
            }
        }
    }
}
