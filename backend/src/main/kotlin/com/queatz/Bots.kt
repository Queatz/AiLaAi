package com.queatz

import com.queatz.db.BotConfigField
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes

@Serializable
data class BotDetails(
    val name: String? = null,
    val description: String? = null,
    val keywords: List<String>? = null,
    val config: List<BotConfigField>? = null,
)

class Bots {
    private val http = HttpClient(CIO) {
        expectSuccess = true
        engine {
            requestTimeout = 2.minutes.inWholeMilliseconds
        }
    }

    suspend fun details(url: String): BotDetails = http.get(url).body()
}
