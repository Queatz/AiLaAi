package com.queatz.api

import com.queatz.db.PlatformConfig
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
        get("/platform") {
            hosts {
                platform.config
            }
        }

        post("/platform") {
            hosts {
                val updates = call.receive<PlatformConfig>()
                var config = platform.config

                if (updates.inviteOnly != null) {
                    config = config.copy(inviteOnly = updates.inviteOnly)
                }

                if (updates.hosts != null) {
                    config = config.copy(hosts = updates.hosts)
                }

                platform.config = config
            }
        }
    }
}

private suspend inline fun <reified T : Any> PipelineContext<*, ApplicationCall>.hosts(block: () -> T) {
    respond {
        if (platform.config.hosts.isNullOrEmpty() || me.id!! in platform.config.hosts!!) {
            block()
        } else {
            HttpStatusCode.BadRequest.description("Not a platform host")
        }
    }
}
