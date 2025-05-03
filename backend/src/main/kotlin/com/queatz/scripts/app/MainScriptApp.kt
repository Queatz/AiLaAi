package com.queatz.scripts.app

import ScriptApp
import com.queatz.save
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes

class MainScriptApp(
    private val script: String,
    me: String?
) : ScriptApp {

    private val http = HttpClient(Java) {
        expectSuccess = true

        engine {
            protocolVersion = java.net.http.HttpClient.Version.HTTP_2
        }
    }

    override suspend fun download(
        url: String,
        name: String,
    ): String {
        return http
            .get(url)
            .bodyAsBytes()
            .save("script/$script", name)
    }
}
