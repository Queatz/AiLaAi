package com.queatz.api

import com.queatz.db.UploadResponse
import com.queatz.plugins.me
import com.queatz.plugins.respond
import com.queatz.receiveFiles
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.uploadRoutes() {
    authenticate {
        post("/upload/photos") {
            respond {
                var urls = emptyList<String>()
                call.receiveFiles("photo", "upload-${me.id}") { it, _ ->
                    urls = it
                }

                UploadResponse(urls)
            }
        }
    }
}
