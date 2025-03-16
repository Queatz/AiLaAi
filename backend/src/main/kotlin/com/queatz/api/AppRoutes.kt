package com.queatz.api

import com.queatz.db.App
import com.queatz.db.AppDetailsBody
import com.queatz.db.apps
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.meOrNull
import com.queatz.plugins.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.appRoutes() {
    get("apps/{id}") {
        respond {
            db.document(App::class, parameter("id"))?.takeIf {
                it.isVisibleTo(meOrNull?.id)
            } ?: HttpStatusCode.NotFound
        }
    }

    get("apps") {
        respond {
            db.apps(meOrNull?.id)
        }
    }

    authenticate {
        post("apps") {
            respond {
                val body = call.receive<AppDetailsBody>()

                val url = body.url.trimEnd('/')

                if (!url.startsWith("https://", ignoreCase = true)) {
                    return@respond HttpStatusCode.BadRequest.description("Param 'url' must begin with \"https://\"")
                }
            }
        }
    }
}

private fun App.isVisibleTo(personId: String?) = creator == personId || open == true
