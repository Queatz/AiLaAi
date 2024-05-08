package com.queatz.api

import com.queatz.db.Comment
import com.queatz.db.Person
import com.queatz.db.asId
import com.queatz.db.comment
import com.queatz.parameter
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

fun Route.commentRoutes() {
    authenticate(optional = true) {
        get("/comments/{id}") {
            respond {
                db.comment(parameter("id")) ?: HttpStatusCode.NotFound
            }
        }
    }

    authenticate {
        post("/comments/{id}/reply") {
            respond {
                val onComment = db.document(Comment::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound

                val newComment = call.receive<Comment>()

                if (newComment.comment.isNullOrBlank()) {
                    return@respond HttpStatusCode.BadRequest.description("Missing comment")
                }

                val comment = db.insert(
                    Comment().apply {
                        comment = newComment.comment
                        from = me.id!!.asId(Person::class)
                        to = onComment.id!!.asId(Comment::class)
                    }
                )

                // todo notify

                comment
            }
        }
    }
}
