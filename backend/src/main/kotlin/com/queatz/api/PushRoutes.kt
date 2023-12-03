package com.queatz.api

import com.queatz.db.DeviceType
import com.queatz.db.device
import com.queatz.parameter
import com.queatz.plugins.db
import com.queatz.plugins.json
import com.queatz.plugins.push
import com.queatz.push.PushData
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.encodeToString
import kotlin.time.Duration.Companion.seconds

private sealed class SseEvent {
    object Heartbeat : SseEvent()
    class Push(val data: PushData) : SseEvent()
}

private suspend fun ApplicationCall.respondSse(events: Flow<PushData>) {
    response.cacheControl(CacheControl.NoCache(null))
    response.header("X-Accel-Buffering", "no")
    respondBytesWriter(contentType = ContentType.Text.EventStream) {
        merge<SseEvent>(
            events.map { SseEvent.Push(it) },
            flow {
                while (true) {
                    delay(30.seconds)
                    emit(SseEvent.Heartbeat)
                }
            }
        ).collect { event ->
            when (event) {
                is SseEvent.Push -> {
                    for (dataLine in json.encodeToString(event.data).lines()) {
                        writeStringUtf8("data: $dataLine\n")
                    }
                    writeStringUtf8("\n")
                    flush()
                }
                is SseEvent.Heartbeat -> {
                    writeStringUtf8("event: heartbeat\n")
                    writeStringUtf8("\n")
                    flush()
                }
            }
        }
    }
}

fun Route.pushRoutes() {
    get("/push/{token}") {
        val token = parameter("token")
        val device = db.device(DeviceType.Web, token)
        call.respondSse(push.flow(device.id!!))
    }
}
