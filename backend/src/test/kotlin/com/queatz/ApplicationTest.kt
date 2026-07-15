package com.queatz

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import io.ktor.http.*
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        client.get("/hi").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("{ \"hi\": true }", bodyAsText())
        }
    }

    @Test
    fun testAssistantWebSocketUnauthorized() = testApplication {
        application {
            module()
        }
        val client = createClient {
            install(WebSockets)
        }
        client.webSocket("/ai/assistant") {
            val reason = closeReason.await()
            assertEquals(CloseReason.Codes.VIOLATED_POLICY.code.toInt(), reason?.code?.toInt())
        }
    }
}
