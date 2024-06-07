package com.queatz.api

import com.queatz.db.Crash
import com.queatz.db.recentCrashes
import com.queatz.plugins.db
import com.queatz.plugins.meOrNull
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.crashRoutes() {
    authenticate(optional = true) {
        post("/crash") {
            respond {
                db.insert(
                    Crash(
                        details = call.receive<Crash>().details,
                        person = meOrNull?.id
                    )
                )
                HttpStatusCode.NoContent
            }
        }
    }

    get("/crash") {
        respond {
            db.recentCrashes(call.parameters["limit"]?.toInt() ?: 20)
        }
    }
}
