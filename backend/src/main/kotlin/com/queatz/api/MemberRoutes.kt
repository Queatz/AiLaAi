package com.queatz.api

import com.queatz.db.*
import com.queatz.parameter
import com.queatz.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.memberRoutes() {
    authenticate {
        post("/members") {
            respond {
                val newMember = call.receive<Member>()
                val group = db.group(me.id!!, newMember.to!!)
                val person = db.document(Person::class, newMember.from!!)
                if (newMember.id != null) {
                    HttpStatusCode.BadRequest.description("'id' cannot be specified")
                } else if (newMember.from == null) {
                    HttpStatusCode.BadRequest.description("'from' is missing")
                } else if (newMember.to == null) {
                    HttpStatusCode.BadRequest.description("'to' is missing")
                } else if (db.member(newMember.from!!, newMember.to!!) != null) {
                    HttpStatusCode.Forbidden.description("Member is already in group")
                } else if (group == null) {
                    HttpStatusCode.NotFound.description("Group not found")
                } else if (person == null) {
                    HttpStatusCode.NotFound.description("Person not found")
                } else {
                    app.createMember(newMember.from!!, newMember.to!!, host = false)
                    notify.newMember(me, person, group.group!!)
                }
            }
        }

        post("/members/{id}") {
            respond {
                val member = db.document(Member::class, parameter("id"))

                val update = call.receive<Member>()

                if (member == null) {
                    HttpStatusCode.NotFound
                } else {
                    if (member.from == me.id!!.asId(Person::class)) {

                        if (update.hide != null) {
                            member.hide = update.hide
                        }

                        if (update.snoozedUntil != null) {
                            member.snoozedUntil = update.snoozedUntil
                        }

                        if (update.snoozed != null) {
                            member.snoozed = update.snoozed?.takeIf { it }
                            member.snoozedUntil = null
                        }

                        db.update(member)

                        HttpStatusCode.NoContent
                    } else {
                        val myMember = db.member(me.id!!, member.to!!)
                        if (myMember?.host == true) {
                            if (update.host == true) {
                                member.host = update.host
                            }

                            db.update(member)

                            HttpStatusCode.NoContent
                        } else {
                            HttpStatusCode.Forbidden
                        }
                    }
                }
            }
        }

        post("/members/{id}/delete") {
            respond {
                val member = db.document(Member::class, parameter("id"))
                val isMe = member?.from == me.id!!.asId(Person::class)

                if (member == null) {
                    HttpStatusCode.NotFound
                } else if (!isMe && !isGroupHost(me.id!!, member.to!!)) {
                    HttpStatusCode.BadRequest.description("Only hosts can remove a member")
                } else if (!isMe && member.host == true) {
                    HttpStatusCode.BadRequest.description("Member is a host")
                } else {
                    member.hide = true
                    member.gone = true
                    if (member.host == true) {
                        member.host = false
                    }
                    db.update(member)
                    HttpStatusCode.NoContent
                }
            }
        }
    }
}

fun isGroupHost(person: String, group: String) = db.member(person, group)?.host == true
