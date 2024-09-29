package com.queatz.api

import com.queatz.accounts
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.accountRoutes() {
    authenticate {
        get("/account") {
            respond {
                accounts.account(me.id!!)
            }
        }
    }
}
