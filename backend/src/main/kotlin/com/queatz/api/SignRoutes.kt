package com.queatz.api

import com.queatz.db.*
import com.queatz.plugins.app
import com.queatz.plugins.db
import com.queatz.plugins.jwt
import com.queatz.plugins.me
import com.queatz.plugins.platform
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

fun Route.signRoutes() {
    post("/sign/up") {
        respond {
            val code = call.receive<SignUpRequest>().code

            if (code == null) {
                if (platform.config.inviteOnly == true) {
                    HttpStatusCode.BadRequest.description("Missing invite code")
                } else {
                    val person = db.insert(
                        Person(
                            seen = Clock.System.now()
                        )
                    )
                    TokenResponse(jwt(person.id!!))
                }
            } else if (code == "000000" && db.totalPeople == 0) {
                val person = db.insert(
                    Person(
                        seen = Clock.System.now()
                    )
                )
                TokenResponse(jwt(person.id!!))
            } else {
                val invite = db.invite(code)

                if (invite == null) {
                    HttpStatusCode.NotFound.description("Invite not found")
                } else {
                    // Quick invites expire in 48 hours
                    if (invite.expiry == null && invite.createdAt!! < Clock.System.now().minus(2.days)) {
                        db.delete(invite)
                        HttpStatusCode.NotFound.description("Invite expired")
                    }

                    // Ensure the invite is not expired
                    if (invite.expiry != null) {
                        if (invite.expiry!! < Clock.System.now()) {
                            db.delete(invite)
                            return@respond HttpStatusCode.Forbidden.description(
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
                            db.delete(invite)
                            return@respond HttpStatusCode.Forbidden.description(
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
                            return@respond HttpStatusCode.Forbidden.description(
                                "The invite has already been used the maximum number of times"
                            )
                        }
                    } else {
                        db.delete(invite)
                    }

                    // Create the new user
                    val person = db.insert(
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

                    // Sign in the new user
                    TokenResponse(jwt(person.id!!))
                }
            }
        }
    }

    post("/sign/in") {
        respond {
            val req = call.receive<SignInRequest>()

            val transfer = req.code?.let(db::transferWithCode)
            val linkDeviceToken = req.link?.let(db::linkDeviceToken)?.takeIf { it.person != null }

            if (transfer != null) {
                val person = db.document(Person::class, transfer.person!!)

                if (person == null) {
                    HttpStatusCode.NotFound.description("Transfer code is orphaned")
                } else {
                    TokenResponse(jwt(person.id!!))
                }
            } else if (linkDeviceToken != null) {
                val person = db.document(Person::class, linkDeviceToken.person!!)

                if (person == null) {
                    HttpStatusCode.NotFound.description("Link device token is orphaned")
                } else {
                    db.delete(linkDeviceToken)
                    TokenResponse(jwt(person.id!!))
                }
            } else {
                HttpStatusCode.NotFound
            }
        }
    }
}
