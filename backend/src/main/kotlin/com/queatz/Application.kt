package com.queatz

import com.queatz.plugins.configureHTTP
import com.queatz.plugins.configureRouting
import com.queatz.plugins.configureSecurity
import com.queatz.plugins.configureSerialization
import io.ktor.server.application.Application
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.util.TimeZone

fun main() {
    System.setProperty("user.timezone", "UTC")
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    embeddedServer(
        factory = Netty,
        module = Application::module,
        configure = {
            connector {
                port = 8080
                host = "0.0.0.0"
            }

            runningLimit = 1024
            responseWriteTimeoutSeconds = 120
        }
    ).start(wait = true)
}


fun Application.module() {
    configureHTTP()
    configureSerialization()
    configureSecurity()
    configureRouting()
}
