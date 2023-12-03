package com.queatz.api

import com.queatz.db.AppFeedback
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.appFeedbackRoutes() {
    authenticate {
        post("/feedback") {
            respond {
                db.insert(call.receive<AppFeedback>().let {
                    AppFeedback(
                        feedback = it.feedback,
                        type = it.type,
                        person = me.id!!
                    )
                })
                HttpStatusCode.OK
            }
        }
    }
}
