package com.queatz.api

import com.queatz.db.*
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.auth.*
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

        post("/messages/{id}/delete") {
            respond {
                val message = db.document(Message::class, parameter("id"))

                if (message == null) {
                    HttpStatusCode.NotFound
                } else {
                    val member = db.document(Member::class, message.member!!)

                    // Todo, also check group owner(s), since they can delete the message
                    if (member?.from != me.id!!.asId(Person::class)) {
                        HttpStatusCode.Forbidden
                    } else {
                        db.delete(message)
                        HttpStatusCode.NoContent
                    }
                }
            }
        }
    }
}
