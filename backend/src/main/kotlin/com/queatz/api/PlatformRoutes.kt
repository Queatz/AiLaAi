package com.queatz.api

import com.queatz.db.Person
import com.queatz.db.PlatformConfig
import com.queatz.db.PlatformMeResponse
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.platform
import com.queatz.plugins.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext

fun Route.platformRoutes() {
    authenticate {
        get("/platform/me") {
            respond {
                PlatformMeResponse(me.isPlatformHost())
            }
        }

        get("/platform") {
            hosts {
                platform.config
            }
        }

        post("/platform") {
            hosts {
                val updates = call.receive<PlatformConfig>()
                val config = platform.config

                if (updates.inviteOnly != null) {
                    config.inviteOnly = updates.inviteOnly
                }

                if (updates.hosts != null) {
                    if (updates.hosts!!.any { db.document(Person::class, it) == null }) {
                        return@hosts HttpStatusCode.BadRequest.description("A host was not found")
                    }
                    config.hosts = updates.hosts?.distinct()
                }

                db.update(config)
            }
        }
    }
}

internal suspend inline fun <reified T : Any> PipelineContext<*, ApplicationCall>.hosts(block: () -> T) {
    respond {
        if (me.isPlatformHost()) {
            block()
        } else {
            HttpStatusCode.BadRequest.description("Not a platform host")
        }
    }
}

private fun Person.isPlatformHost(): Boolean =
    platform.config.hosts.isNullOrEmpty() || id!! in platform.config.hosts!!
