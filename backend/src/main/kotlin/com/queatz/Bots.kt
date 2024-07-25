package com.queatz

import com.queatz.db.BotConfigField
import com.queatz.db.BotConfigValue
import com.queatz.plugins.json
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

@Serializable
data class BotDetails(
    val name: String? = null,
    val description: String? = null,
    val keywords: List<String>? = null,
    val config: List<BotConfigField>? = null,
)

@Serializable
data class InstallBotResponse(
    val token: String
)

@Serializable
data class InstallBotBody(
    val groupId: String,
    val groupName: String,
    val webhook: String,
    val config: List<BotConfigValue>? = null,
    val secret: String? = null
)

@Serializable
data class ReinstallBotBody(
    val config: List<BotConfigValue>? = null,
)

@Serializable
data class MessageBotResponse(
    val success: Boolean? = null,
    val note: String? = null,
    val actions: List<BotAction>? = null
)

@Serializable
data class BotAction(
    val message: String? = null
)

@Serializable
data class MessageBotBody(
    val message: String? = null
)

class Bots {
    private val http = HttpClient(CIO) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }

        engine {
            requestTimeout = 2.minutes.inWholeMilliseconds
        }
    }

    suspend fun details(url: String): BotDetails = http.get(url).body()

    suspend fun install(url: String, body: InstallBotBody): InstallBotResponse =
        http.post("$url/install") {
            setBody(body)
        }.body()

    suspend fun reinstall(url: String, authToken: String, body: ReinstallBotBody): HttpStatusCode =
        http.post("$url/reinstall") {
            bearerAuth(authToken)
            setBody(body)
        }.body()

    suspend fun uninstall(url: String, authToken: String): HttpStatusCode =
        http.post("$url/uninstall") {
            bearerAuth(authToken)
        }.body()

    suspend fun message(url: String, authToken: String, body: MessageBotBody): MessageBotResponse =
        http.post("$url/message") {
            bearerAuth(authToken)
            setBody(body)
        }.body()
}
