package com.queatz.api

import com.queatz.db.Invite
import com.queatz.db.invite
import com.queatz.db.member
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlin.random.Random

fun Route.inviteRoutes() {

    get("/invite/{code}") {
        respond {
            db.invite(parameter("code")) ?: HttpStatusCode.NotFound
        }
    }

    /**
     * Create a simple invite that has a code the other person can enter.
     */
    authenticate {
        get("/invite") {
            respond {
                db.insert(
                    Invite(
                        person = me.id!!,
                        code = Random.code()
                    )
                )
            }
        }

        post("/invite") {
            respond {
                val invite = call.receive<Invite>()

                if (invite.code != null) {
                    return@respond HttpStatusCode.BadRequest.description("Parameter 'code' cannot be specified")
                }

                if (invite.person != null) {
                    return@respond HttpStatusCode.BadRequest.description("Parameter 'person' cannot be specified")
                }

                if (invite.remaining != null) {
                    return@respond HttpStatusCode.BadRequest.description("Parameter 'remaining' cannot be specified")
                }

                if (invite.group != null) {
                    if (
                        db.member(
                            person = me.id!!,
                            group = invite.group!!
                        )?.host != true
                    ) {
                        return@respond HttpStatusCode.Forbidden.description("You are not the group's host")
                    }
                }

                db.insert(
                    Invite(
                        person = me.id!!,
                        group = invite.group,
                        about = invite.about,
                        expiry = invite.expiry,
                        total = invite.total,
                        remaining = invite.total,
                        code = Random.code(length = 32)
                    )
                )
            }
        }

        post("/invite/{id}") {
            respond {
                val invite = db.document(Invite::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                val updated = call.receive<Invite>()

                if (!invite.canEdit(me.id!!)) {
                    return@respond HttpStatusCode.Forbidden
                }

                if (updated.about != null) {
                    invite.about = updated.about
                }

                if (updated.expiry != null) {
                    invite.expiry = updated.expiry
                }

                if (updated.remaining != null) {
                    invite.remaining = updated.remaining
                }

                if (updated.total != null) {
                    invite.total = updated.total
                }

                db.update(invite)
            }
        }

        post("/invite/{id}/delete") {
            respond {
                val invite = db.document(Invite::class, parameter("id"))
                    ?: return@respond HttpStatusCode.NotFound

                if (!invite.canEdit(me.id!!)) {
                    return@respond HttpStatusCode.Forbidden
                }

                db.delete(invite)

                HttpStatusCode.OK
            }
        }
    }
}

private fun Invite.canEdit(me: String): Boolean = if (group != null) {
    db.member(person = me, group = group!!)?.host == true
} else {
    person == me
}

private fun Random.code(length: Int = 6) =
    (1..length).joinToString("") { "${nextInt(9)}" }
