package com.queatz.api

import com.queatz.cropTransparentBackground
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
                var crop = false
                call.receiveFiles("photo", "upload-${meOrNull?.id}") { it, params ->
                    urls = it
                    removeBackground = params.containsKey("removeBackground")
                    crop = params.containsKey("crop")
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

                if (crop) {
                    runCatching {
                        urls = urls.map { url ->
                            val file = File(".$url")

                            if (file.hasTransparency()) {
                                // Read the file bytes
                                val bytes = file.readBytes()

                                // Crop the transparent background
                                val (croppedBytes, _) = bytes.cropTransparentBackground()

                                // Save the cropped image
                                val croppedPath = url.substringBeforeLast(".") + "-cropped.png"
                                val croppedFile = File(".$croppedPath")
                                croppedFile.writeBytes(croppedBytes)

                                croppedPath
                            } else {
                                url
                            }
                        }
                    }.onFailure {
                        it.printStackTrace()
                        println("Crop transparent background error:")
                        println(it.message)
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
