package com.queatz

import com.queatz.db.Message
import com.queatz.db.UrlAttachment
import com.queatz.db.extractUrls
import com.queatz.plugins.db
import com.queatz.plugins.json
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.util.logging.Logger

val urlAttachmentFetcher = UrlAttachmentFetcher()

class UrlAttachmentFetcher {

    private lateinit var scope: CoroutineScope

    private val http = HttpClient(Java) {
        engine {
            protocolVersion = java.net.http.HttpClient.Version.HTTP_2
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    fun start(scope: CoroutineScope) {
        this.scope = scope
    }

    fun handle(message: Message) {
        message.text?.extractUrls()?.takeIf { it.isNotEmpty() }?.let { urls ->
            scope.launch {
                val responses = urls.map { async { it to try { http.get(it) } catch (_: Throwable) { null } } }.awaitAll()
                responses.forEach { (url, response) ->
                    response ?: return@forEach

                    if (!response.status.isSuccess()) {
                        Logger.getAnonymousLogger().info("Failed to fetch Open Graph data: $response")
                        return@forEach
                    }

                    val openGraphData = response.bodyAsText().extractOpenGraphData()

                    if (openGraphData.title == null || openGraphData.description == null || openGraphData.image == null) {
                        return@forEach
                    }

                    message.attachments = (message.attachments ?: emptyList()) + json.encodeToString(
                        UrlAttachment(
                            url = url,
                            title = openGraphData.title,
                            description = openGraphData.description,
                            image = openGraphData.image
                        )
                    )

                    db.update(message)

                    // todo notify message updated
                }
            }
        }
    }
}
