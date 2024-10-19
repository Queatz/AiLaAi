package com.queatz.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import org.slf4j.event.Level

fun Application.configureHTTP() {
    install(CORS) {
        allowNonSimpleContentTypes = true
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        anyHost()
    }
    install(Compression)
    install(DefaultHeaders)
    install(CallLogging) {
        level = Level.WARN
    }
}
