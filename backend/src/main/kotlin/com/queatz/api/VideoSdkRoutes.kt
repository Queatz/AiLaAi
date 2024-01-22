package com.queatz.api

import com.queatz.VideoSdkSessionsResponse
import com.queatz.groupCall
import com.queatz.plugins.json
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.util.logging.Logger

@Serializable
data class VideoSdkSessionWebhookEvent(
    val sessionId: String,
    val meetingId: String,
    val start: String? = null,
    val end: String? = null,
)

@Serializable
data class VideoSdkParticipantWebhookEvent(
    val meetingId: String,
    val sessionId: String,
    val participantId: String,
    val participantName: String,
)

fun Route.videoSdkRoutes() {
    post("/videosdk/webhook") {
        Logger.getAnonymousLogger().warning("VideoSDK (webook)")
        // todo: verify: https://docs.videosdk.live/api-reference/realtime-communication/webhook-verification

        val eventJson = call.receive<JsonObject>()
        val webhookType = eventJson["webhookType"]?.jsonPrimitive?.contentOrNull

        val event = when (webhookType) {
            "participant-joined" -> json.decodeFromJsonElement<VideoSdkParticipantWebhookEvent>(eventJson["data"]!!)
            "participant-left" -> json.decodeFromJsonElement<VideoSdkParticipantWebhookEvent>(eventJson["data"]!!)
            "session-started" -> json.decodeFromJsonElement<VideoSdkSessionWebhookEvent>(eventJson["data"]!!)
            "session-ended" -> json.decodeFromJsonElement<VideoSdkSessionWebhookEvent>(eventJson["data"]!!)
            else -> return@post
        }

        when (event) {
            is VideoSdkParticipantWebhookEvent -> {
                groupCall.updateParticipantCount(event.meetingId, event.sessionId)
            }
            is VideoSdkSessionWebhookEvent -> {
                if (event.end != null) {
                    groupCall.updateParticipantCount(event.meetingId, event.sessionId)
                } else {
                    groupCall.updateParticipantCount(event.meetingId, VideoSdkSessionsResponse())
                }
            }
        }

        call.respond(HttpStatusCode.OK)
    }
}
