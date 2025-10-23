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
import kotlin.time.Clock
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
                    val (person, error) = invite.use()

                    error?.let {
                        return@respond it
                    }

                    // Sign in the new user
                    TokenResponse(jwt(person!!.id!!))
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
