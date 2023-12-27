package com.queatz.api

import com.queatz.parameter
import com.queatz.plugins.me
import com.queatz.plugins.respond
import com.queatz.receiveFile
import com.queatz.receiveFiles
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.profileRoutes() {
    authenticate {
        post("/profile/{id}/content/photos") {
            respond {
                if (me.id != parameter("id")) {
                    return@respond HttpStatusCode.BadRequest
                }

                var result: List<String>? = null
                call.receiveFiles("photo", "profile-content-${me.id}") { photosUrls, _ ->
                    result = photosUrls
                }
                result ?: HttpStatusCode.InternalServerError
            }
        }

        post("/profile/{id}/content/audio") {
            respond {
                if (me.id != parameter("id")) {
                    return@respond HttpStatusCode.BadRequest
                }

                var result: String? = null
                call.receiveFile("audio", "profile-content-${me.id}") { audioUrl, _ ->
                    result = audioUrl
                }
                result ?: HttpStatusCode.InternalServerError
            }
        }
    }
}
