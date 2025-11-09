package com.queatz.api

import com.queatz.db.activeCallsOfPerson
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.callRoutes() {
    authenticate {
        get("/calls") {
            respond {
                db.activeCallsOfPerson(me.id!!)
            }
        }

        get("/calls/{id}") {
            respond {
                db.document(com.queatz.db.Call::class, parameter("id")) ?: io.ktor.http.HttpStatusCode.NotFound
            }
        }
    }
}
