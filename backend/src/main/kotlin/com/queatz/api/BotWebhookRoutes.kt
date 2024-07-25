package com.queatz.api

import com.queatz.plugins.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.botWebhookRoutes() {
    get("/bot/{token}") {
        respond {

        }
    }

    post("/bot/{token}") {
        respond {

        }
    }
}
