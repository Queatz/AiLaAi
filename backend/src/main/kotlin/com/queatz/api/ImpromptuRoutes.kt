package com.queatz.api

import com.queatz.db.Db
import com.queatz.db.Impromptu
import com.queatz.db.ImpromptuHistory
import com.queatz.db.ImpromptuSeek
import com.queatz.db.getImpromptu
import com.queatz.db.getImpromptuHistory
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.impromptuRoutes() {
    authenticate {
        // Get a person's impromptu settings
        get("me/impromptu") {
            val me = me
            respond {
                db.getImpromptu(me.id!!) ?: Impromptu(
                    person = me.id,
                ).let {
                    db.insert(it)
                }
            }
        }

        // Update a person's impromptu settings
        post("me/impromptu") {
            val me = me
            val impromptu = call.receive<Impromptu>()
            respond {
                db.updateImpromptu(me.id!!, impromptu)
            }
        }

        // Add an ImpromptuSeek
        post("me/impromptu/seek") {
            val me = me
            val impromptuSeek = call.receive<ImpromptuSeek>()
            respond {
                db.insert(
                    ImpromptuSeek().apply {
                        person = me.id
                        name = impromptuSeek.name?.trim()
                        offer = impromptuSeek.offer
                        radius = impromptuSeek.radius
                        expiresAt = impromptuSeek.expiresAt
                    }
                )
            }
        }

        // Update an ImpromptuSeek
        post("me/impromptu/seek/{id}") {
            val me = me
            val id = call.parameters["id"]!!
            val impromptuSeek = call.receive<ImpromptuSeek>()
            respond {
                val existingSeek = db.document(ImpromptuSeek::class, id)
                if (existingSeek == null || existingSeek.person != me.id) {
                    HttpStatusCode.NotFound
                } else {
                    db.update(
                        existingSeek.apply {
                            name = impromptuSeek.name ?: name
                            offer = impromptuSeek.offer ?: offer
                            radius = impromptuSeek.radius ?: radius
                            expiresAt = impromptuSeek.expiresAt ?: expiresAt
                        }
                    )
                }
            }
        }

        // Delete an ImpromptuSeek
        post("me/impromptu/seek/{id}/delete") {
            val me = me
            val id = call.parameters["id"]!!
            respond {
                val existingSeek = db.document(ImpromptuSeek::class, id)
                if (existingSeek == null || existingSeek.person != me.id) {
                    HttpStatusCode.NotFound
                } else {
                    db.delete(existingSeek)
                    HttpStatusCode.OK
                }
            }
        }

        // Get a person's impromptu history
        get("me/impromptu/history") {
            val me = me
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            respond {
                db.getImpromptuHistory(me.id!!, page, pageSize)
            }
        }

        // Delete an impromptu history
        post("me/impromptu/history/{id}/delete") {
            val me = me
            respond {
                if (
                    db.deleteImpromptuHistory(
                        id = call.parameters["id"]!!,
                        person = me.id!!
                    )
                ) {
                    HttpStatusCode.OK
                } else {
                    HttpStatusCode.NotFound
                }
            }
        }
    }
}

/**
 * Create or update a person's impromptu settings
 */
private fun Db.updateImpromptu(person: String, impromptu: Impromptu): Impromptu {
    val existingImpromptu = getImpromptu(person)

    return if (existingImpromptu == null) {
        insert(
            Impromptu().apply {
                this.person = person
                mode = impromptu.mode
                updateLocation = impromptu.updateLocation
                notificationType = impromptu.notificationType
            }
        )
    } else {
        update(
            existingImpromptu.apply {
                if (impromptu.mode != null) {
                    mode = impromptu.mode
                }
                if (impromptu.updateLocation != null) {
                    updateLocation = impromptu.updateLocation
                }
                if (impromptu.notificationType != null) {
                    notificationType = impromptu.notificationType
                }
            }
        )
    }
}

private fun Db.deleteImpromptuHistory(id: String, person: String): Boolean {
    val history = document(ImpromptuHistory::class, id)
    return if (history != null && history.person == person) {
        update(
            history.apply {
                gone = true
            }
        )
        true
    } else {
        false
    }
}
