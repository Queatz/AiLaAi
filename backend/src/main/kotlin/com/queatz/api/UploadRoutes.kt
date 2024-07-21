package com.queatz.api

import com.queatz.db.UploadResponse
import com.queatz.hasTransparency
import com.queatz.plugins.ai
import com.queatz.plugins.me
import com.queatz.plugins.respond
import com.queatz.receiveFiles
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import java.io.File

fun Route.uploadRoutes() {
    authenticate {
        post("/upload/photos") {
            respond {
                var urls = emptyList<String>()
                var removeBackground = false
                call.receiveFiles("photo", "upload-${me.id}") { it, params ->
                    urls = it
                    removeBackground = params.containsKey("removeBackground")
                }

                val file = File(".$it")

                if (removeBackground && !file.hasTransparency()) {
                    runCatching {
                        urls = urls.map {
                            ai.removeBackground(file)
                        }
                    }.onFailure {
                        it.printStackTrace()
                        (it as? ServerResponseException)?.let {
                            println("Remove background error:")
                            println(it.response.bodyAsText())
                        }
                    }
                }

                UploadResponse(urls)
            }
        }
    }
}
