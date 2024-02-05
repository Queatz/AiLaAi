package com.queatz.plugins

import com.queatz.api.*
import com.queatz.groupCall
import com.queatz.remind
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.days

fun Application.configureRouting() {
    launch {
        // Delete all expired invite codes every hour
        // Delete all expired link device tokens every hour
    }

    push.start(this)
    groupCall.start(this)
    remind.start(this)

    routing {
        get("/hi") { call.respondText("{\"hi\": true}") }
        signRoutes()
        meRoutes()
        cardRoutes()
        categoryRoutes()
        groupRoutes()
        peopleRoutes()
        profileRoutes()
        reminderRoutes()
        memberRoutes()
        messageRoutes()
        wildRoutes()
        hmsRoutes()
        crashRoutes()
        statsRoutes()
        storyRoutes()
        stickerRoutes()
        appFeedbackRoutes()
        reportRoutes()
        exportDataRoutes()
        linkDeviceRoutes()
        pushRoutes()
        joinRequestRoutes()
        widgetRoutes()
        aiRoutes()
        uploadRoutes()
        videoSdkRoutes()
        callRoutes()
        itemRoutes()

        staticFiles("/static", File("static")) {
            install(CachingHeaders) {
                options { _, outgoingContent ->
                    when (outgoingContent.contentType?.contentType) {
                        ContentType.Image.Any.contentType -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 60.days.inWholeSeconds.toInt()))
                        else -> null
                    }
                }
            }
        }
    }
}

suspend inline fun <reified T : Any> PipelineContext<*, ApplicationCall>.respond(block: () -> T) {
    call.respond(block())
}
