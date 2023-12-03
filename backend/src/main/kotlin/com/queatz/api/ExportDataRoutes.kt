package com.queatz.api

import com.queatz.db.*
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.server.auth.*
import io.ktor.server.routing.*


fun Route.exportDataRoutes() {
    authenticate {
        get("/export-data") {
            respond {
                val me = me
                ExportDataResponse(
                    db.profile(me.id!!),
                    db.cardsOfPerson(me.id!!),
                    db.storiesOfPerson(me.id!!)
                )
            }
        }
    }
}
