package com.queatz.api

import com.queatz.db.Invite
import com.queatz.db.Person
import com.queatz.db.invite
import com.queatz.db.member
import com.queatz.parameter
import com.queatz.plugins.app
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.datetime.Clock
import kotlin.random.Random
import kotlin.time.Duration.Companion.days

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

        post("/invite/{code}/use") {
            respond {
                val invite = db.invite(parameter("code"))

                if (invite == null) {
                    HttpStatusCode.NotFound.description("Invite not found")
                } else {
                    val (_, error) = invite.use(me = me)

                    error?.let {
                        return@respond it
                    }

                    HttpStatusCode.OK
                }
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

fun Invite.use(me: Person? = null): Pair<Person?, HttpStatusCode?> {
    val invite = this

    // You can't use your own invite
    if (invite.person == me?.id) {
        return null to HttpStatusCode.Forbidden.description("You can't use your own invite")
    }

    // Quick invites expire in 48 hours
    if (invite.expiry == null && invite.createdAt!! < Clock.System.now().minus(2.days)) {
        db.delete(invite)
        return null to HttpStatusCode.NotFound.description("Invite expired")
    }

    // Ensure the invite is not expired
    if (invite.expiry != null) {
        if (invite.expiry!! < Clock.System.now()) {
            db.delete(invite)
            return null to HttpStatusCode.Forbidden.description(
                "The invite has expired"
            )
        }
    }

    // Ensure the invite's creator is still the group's host
    if (invite.group != null) {
        if (
            db.member(
                person = invite.person!!,
                group = invite.group!!
            )?.host != true
        ) {
            return null to HttpStatusCode.Forbidden.description(
                "The person who created this invite is no longer the group's host"
            )
        }
    }

    // Ensure there are remaining uses of this invite
    if (invite.total != null) {
        if ((invite.remaining ?: 0) > 0) {
            // Consume the use
            invite.remaining = invite.remaining!! - 1
            db.update(invite)
        } else {
            db.delete(invite)
            return null to HttpStatusCode.Forbidden.description(
                "The invite has already been used the maximum number of times"
            )
        }
    } else {
        db.delete(invite)
    }

    // Create the new user, if needed
    val person = me ?: db.insert(
        Person(
            seen = Clock.System.now()
        )
    )

    // Add the user to the specified group, if any
    if (invite.group != null) {
        app.createMember(
            person = person.id!!,
            group = invite.group!!
        )
    }

    // If no group was specified, create an initial group with the invite's creator
    else {
        app.createGroup(
            people = listOf(person.id!!, invite.person!!)
        )
    }

    return person.takeIf { it != me } to null
}

private fun Invite.canEdit(me: String): Boolean = if (group != null) {
    db.member(person = me, group = group!!)?.host == true
} else {
    person == me
}

private fun Random.code(length: Int = 6) =
    (1..length).joinToString("") { "${nextInt(9)}" }
