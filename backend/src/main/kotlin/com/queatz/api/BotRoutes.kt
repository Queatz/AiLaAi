package com.queatz.api

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.botRoutes() {
    authenticate {
        get("bots") {

        }

        post("bots") {

        }

        get("bots/{id}") {

        }

        post("bots/{id}") {

        }

        post("bots/{id}/reload") {

        }

        post("bots/{id}/delete") {

        }
    }
}
