package com.queatz.api

import com.queatz.db.*
import com.queatz.parameter
import com.queatz.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock


fun Route.joinRequestRoutes() {
    authenticate {
        get("me/join-requests") {
            respond {
                db.myJoinRequests(me.id!!)
            }
        }

        get("join-requests") {
            respond {
                db.joinRequests(me.id!!)
            }
        }

        post("join-requests") {
            respond {
                val joinRequest = call.receive<JoinRequest>()

                if (joinRequest.person != null) {
                    return@respond HttpStatusCode.BadRequest.description("'person' must not be set")
                }

                val exists = db.joinRequest(me.id!!, joinRequest.group!!) != null
                val group = db.group(me.id!!, joinRequest.group!!)

                if (exists) {
                    HttpStatusCode.BadRequest.description("Already requested")
                } else if (group == null) {
                    HttpStatusCode.NotFound
                } else {
                    group.group?.let { group ->
                        group.seen = Clock.System.now()
                        db.update(group)
                    }
                    db.insert(
                        JoinRequest(
                            person = me.id!!,
                            group = joinRequest.group!!,
                            message = joinRequest.message
                        )
                    ).also {
                        notify.newJoinRequest(me, it, group.group!!)
                    }
                }
            }
        }

        post("join-requests/{id}/accept") {
            respond {
                val joinRequest = db.document(JoinRequest::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound
                if (!isGroupHost(me.id!!, joinRequest.group!!)) {
                    return@respond HttpStatusCode.NotFound
                }

                val group = db.group(me.id!!, joinRequest.group!!)
                val person = db.document(Person::class, joinRequest.person!!)

                if (db.member(joinRequest.person!!, joinRequest.group!!) != null) {
                    HttpStatusCode.Forbidden.description("Member is already in group")
                } else if (group == null) {
                    HttpStatusCode.NotFound.description("Group not found")
                } else if (person == null) {
                    HttpStatusCode.NotFound.description("Person not found")
                } else {
                    app.createMember(joinRequest.person!!, joinRequest.group!!, host = false)
                    db.delete(joinRequest)
                    notify.newMember(me, person, group.group!!)
                    HttpStatusCode.OK
                }
            }
        }

        post("join-requests/{id}/delete") {
            respond {
                val joinRequest = db.document(JoinRequest::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound
                if (joinRequest.person != me.id && !isGroupHost(me.id!!, joinRequest.group!!)) {
                    return@respond HttpStatusCode.NotFound
                }

                db.delete(joinRequest)
                HttpStatusCode.OK
            }
        }
    }
}
