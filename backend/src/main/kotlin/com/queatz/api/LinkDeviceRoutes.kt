package com.queatz.api

import com.queatz.db.LinkDeviceToken
import com.queatz.db.linkDeviceToken
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.meOrNull
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.linkDeviceRoutes() {
    authenticate(optional = true) {
        post("/link-device") {
            respond {
                db.insert(
                    LinkDeviceToken(
                        person = meOrNull?.id,
                        token = (1..128).token(),
                    )
                )
            }
        }

        get("/link-device/{token}") {
            respond {
                db.linkDeviceToken(parameter("token")) ?: HttpStatusCode.NotFound
            }
        }
    }

    authenticate {
        post("/link-device/{token}/confirm") {
            respond {
                db.linkDeviceToken(parameter("token"))?.let {
                    it.person = me.id!!
                    db.update(it)
                } ?: HttpStatusCode.NotFound
            }
        }
    }
}
