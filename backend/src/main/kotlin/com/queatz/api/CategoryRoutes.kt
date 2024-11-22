package com.queatz.api

import com.queatz.db.cardsOfPerson
import com.queatz.db.explore
import com.queatz.db.groupsPlain
import com.queatz.db.reminders
import com.queatz.notBlank
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.defaultNearbyMaxDistanceInMeters
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.categoryRoutes() {
    authenticate {
        get("/categories") {
            respond {
                val geo = parameter("geo").split(",").map { it.toDouble() }

                if (geo.size != 2) {
                    return@respond HttpStatusCode.BadRequest.description("'geo' must be an array of size 2")
                }

                val person = me

                (
                    (
                        (
                            db.explore(
                                person = person.id!!,
                                geo = geo,
                                search = call.parameters["search"]?.notBlank,
                                nearbyMaxDistance = defaultNearbyMaxDistanceInMeters,
                                offset = call.parameters["offset"]?.toInt() ?: 0,
                                limit = call.parameters["limit"]?.toInt() ?: 20
                            ) + db.cardsOfPerson(person.id!!)).flatMap {
                        it.categories ?: emptyList()
                        }
                    ) + db.groupsPlain(person.id!!).flatMap {
                        it.categories ?: emptyList()
                    } + db.reminders(person.id!!, limit = 200).flatMap {
                        it.categories ?: emptyList()
                    }
                )
                    .distinct()
                    .sorted()
            }
        }
    }
}
