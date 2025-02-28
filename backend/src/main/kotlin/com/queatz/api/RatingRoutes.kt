package com.queatz.api


import com.queatz.db.ratings
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.ratingRoutes() {
    authenticate {
        get("/ratings") {
            respond {
                db.ratings(
                    me.id!!,
                    call.parameters["offset"]?.toInt() ?: 0,
                    call.parameters["limit"]?.toInt() ?: 20,
                    call.parameters["descending"]?.toBoolean() != false,
                )
            }
        }
    }
}
