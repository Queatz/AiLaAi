package com.queatz.api

import com.queatz.db.Group
import com.queatz.db.Member
import com.queatz.db.Message
import com.queatz.db.Person
import com.queatz.db.ReactBody
import com.queatz.db.Story
import com.queatz.db.asId
import com.queatz.db.group
import com.queatz.db.member
import com.queatz.db.message
import com.queatz.db.react
import com.queatz.db.reactionsOf
import com.queatz.db.unreact
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.meOrNull
import com.queatz.plugins.notify
import com.queatz.plugins.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.messageRoutes() {
    authenticate {
        get("/messages/{id}") {
            respond {
                val message = db.message(meOrNull?.id?.asId(Person::class), parameter("id"))
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

        post("/messages/{id}/react") {
            respond {
                val message = db.document(Message::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound

                // Ensure the user is a member
                val group = db.group(me.id!!, message.group!!) ?: return@respond HttpStatusCode.NotFound

                val react = call.receive<ReactBody>()

                if (react.remove == true) {
                    db.unreact(
                        from = me.id!!.asId(Person::class),
                        to = message.id!!.asId(Message::class),
                        reaction = react.reaction.reaction!!.take(64)
                    )

                    notifyReaction(me, group.group!!, message, react)
                } else {
                    db.react(
                        from = me.id!!.asId(Person::class),
                        to = message.id!!.asId(Message::class),
                        reaction = react.reaction.reaction!!.take(64),
                        comment = react.reaction.comment
                    )

                    notifyReaction(me, group.group!!, message, react)
                }

                HttpStatusCode.NoContent
            }
        }

        get("/messages/{id}/reactions") {
            respond {
                db.reactionsOf(parameter("id").asId(Message::class))
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

private fun notifyReaction(me: Person, group: Group, message: Message, react: ReactBody) {
    notify.messageReaction(
        group = group,
        person = me,
        message = message,
        react = react
    )
}
