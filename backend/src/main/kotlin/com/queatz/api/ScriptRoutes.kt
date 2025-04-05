package com.queatz.api

import com.queatz.db.RunScriptBody
import com.queatz.db.Script
import com.queatz.db.ScriptResult
import com.queatz.db.allScripts
import com.queatz.db.scriptsOfPerson
import com.queatz.db.searchScripts
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.me
import com.queatz.plugins.meOrNull
import com.queatz.plugins.respond
import com.queatz.scripts.RunScript
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Route.scriptRoutes() {
    authenticate(optional = true) {
        get("/scripts") {
            respond {
                val search = call.parameters["search"]
                val offset = call.parameters["offset"]?.toInt() ?: 0
                val limit = call.parameters["limit"]?.toInt() ?: 20

                if (search.isNullOrBlank()) {
                    db.allScripts(offset, limit)
                } else {
                    db.searchScripts(search, offset, limit)
                }
            }
        }

        get("/scripts/{id}") {
            respond {
                db.document(Script::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound
            }
        }

        post("/scripts/{id}/run") {
            respond {
                val script = db.document(Script::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound
                val data = call.receive<RunScriptBody>()

                withContext(Dispatchers.IO) {
                    RunScript(
                        script = script,
                        data = data.data,
                        useCache = data.useCache != false
                    ).run(meOrNull)
                }
            }
        }
    }

    authenticate {
        post("/scripts") {
            respond {
                val script = call.receive<Script>()

                db.insert(
                    Script(
                        person = me.id!!,
                        name = script.name,
                        source = script.source
                    )
                )
            }
        }

        get("/me/scripts") {
            respond {
                db.scriptsOfPerson(me.id!!)
            }
        }

        post("/scripts/{id}") {
            respond {
                val script = db.document(Script::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound

                if (me.id!! != script.person) {
                    return@respond HttpStatusCode.BadRequest.description("Script is not owned by this person")
                }

                val scriptUpdated = call.receive<Script>()

                if (scriptUpdated.name != null) {
                    script.name = scriptUpdated.name
                }

                if (scriptUpdated.source != null) {
                    script.source = scriptUpdated.source
                }

                if (scriptUpdated.description != null) {
                    script.description = scriptUpdated.description
                }

                if (scriptUpdated.categories != null) {
                    script.categories = scriptUpdated.categories
                }

                db.update(script)
            }
        }

        post("/scripts/{id}/delete") {
            respond {
                val script = db.document(Script::class, parameter("id")) ?: return@respond HttpStatusCode.NotFound

                if (me.id!! != script.person) {
                    return@respond HttpStatusCode.BadRequest.description("Script is not owned by this person")
                }

                db.delete(script)

                HttpStatusCode.OK
            }
        }
    }
}
