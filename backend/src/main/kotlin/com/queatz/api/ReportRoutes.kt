package com.queatz.api

import com.queatz.db.Report
import com.queatz.db.recentReports
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.respond
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Route.reportRoutes() {
    // todo only for admins
    get("/report") {
        respond {
            if (parameter("password") != "letmein") {
                return@respond HttpStatusCode.NotFound
            }

            db.recentReports(call.parameters["limit"]?.toInt() ?: 20)
        }
    }

    authenticate {
        post("/report") {
            respond {
                call.receive<Report>().also { report ->
                    db.insert(
                        Report(
                            reporter = me.id!!,
                            type = report.type!!,
                            entity = report.entity!!,
                            urgent = report.urgent?.takeIf { it },
                            reporterMessage = report.reporterMessage
                        )
                    )
                }
                HttpStatusCode.NoContent
            }
        }
    }
}
