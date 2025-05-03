package com.queatz.api

import com.queatz.db.UploadResponse
import com.queatz.hasTransparency
import com.queatz.plugins.ai
import com.queatz.plugins.meOrNull
import com.queatz.plugins.respond
import com.queatz.receiveFile
import com.queatz.receiveFiles
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.io.File

fun Route.uploadRoutes() {
    authenticate(optional = true) {
        post("/upload/photos") {
            respond {
                var urls = emptyList<String>()
                var removeBackground = false
                call.receiveFiles("photo", "upload-${meOrNull?.id}") { it, params ->
                    urls = it
                    removeBackground = params.containsKey("removeBackground")
                }

                if (removeBackground) {
                    runCatching {
                        urls = urls.map { url ->
                            val file = File(".$url")

                            if (file.hasTransparency()) {
                                url
                            } else {
                                ai.removeBackground(file)
                            }
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

        post("/upload/video") {
            respond {
                var url = ""
                call.receiveFile("video", "upload-${meOrNull?.id}") { fileName, _ ->
                    url = fileName
                }
                UploadResponse(listOf(url))
            }
        }

        post("/upload/audio") {
            respond {
                var url = ""
                call.receiveFile("audio", "upload-${meOrNull?.id}") { fileName, _ ->
                    url = fileName
                }
                UploadResponse(listOf(url))
            }
        }
    }
}
