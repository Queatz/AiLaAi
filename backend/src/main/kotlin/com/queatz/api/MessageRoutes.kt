package com.queatz.api

import com.queatz.db.*
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.call
import io.ktor.server.auth.*
import io.ktor.server.request.receive
import io.ktor.server.routing.*

fun Route.messageRoutes() {
    authenticate {
        get("/messages/{id}") {
            respond {
                val message = db.document(Message::class, parameter("id"))
                val member = message?.group?.let { db.member(me.id!!, it) }

                if (message == null || member == null) {
                    HttpStatusCode.NotFound
                } else {
                    message
                }
            }
        }

        post("/messages/{id}") {
            respond {
                val message = db.document(Message::class, parameter("id"))
                val updatedMessage = call.receive<Message>()

                if (message == null) {
                    HttpStatusCode.NotFound
                } else {
                    val member = db.document(Member::class, message.member!!)
                    val isMember = member?.gone != true && member?.from == me.id!!.asId(Person::class)

                    // Todo, also check group owner(s), since they can delete the message
                    if (isMember || db.member(me.id!!, member!!.to!!)?.host == true) {
                        if (updatedMessage.text != null) {
                            message.text = updatedMessage.text
                        }

                        // todo save history

                        db.update(message)
                    } else {
                        HttpStatusCode.Forbidden
                    }
                }
            }
        }

        post("/messages/{id}/delete") {
            respond {
                val message = db.document(Message::class, parameter("id"))

                if (message == null) {
                    HttpStatusCode.NotFound
                } else {
                    val member = message.member?.let { db.document(Member::class, it) }
                    val isMember = member?.gone != true && member?.from == me.id!!.asId(Person::class)

                    // Todo, also check group owner(s), since they can delete the message
                    if (isMember || db.member(me.id!!, message.group!!)?.host == true) {
                        db.delete(message)
                        HttpStatusCode.NoContent
                    } else {
                        HttpStatusCode.Forbidden
                    }
                }
            }
        }
    }
}
