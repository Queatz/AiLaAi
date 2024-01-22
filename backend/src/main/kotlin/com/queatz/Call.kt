package com.queatz

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.queatz.db.Group
import com.queatz.db.call
import com.queatz.db.callByRoom
import com.queatz.plugins.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.util.*
import io.ktor.util.date.*
import io.vertx.ext.auth.impl.jose.JWS.HS256
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import java.net.http.HttpClient
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.logging.Logger
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

val groupCall = Call()

@Serializable
data class VideoSdkJwtPayload(
    val apikey: String,
    val permissions: List<String> = listOf("allow_join"),
    val version: Int = 2
)

@Serializable
data class VideoSdkRoomResponse(
    val roomId: String
)

@Serializable
data class VideoSdkSession(
    val id: String,
    val status: String,
    val roomId: String,
    val start: String? = null,
    val end: String? = null,
)

@Serializable
data class VideoSdkPageInfo(
    val total: Int? = null
)

@Serializable
data class VideoSdkSessionsResponse(
    val pageInfo: VideoSdkPageInfo? = null,
    val data: List<VideoSdkSession> = emptyList()
)

@Serializable
data class VideoSdkWebhookBody(
    val endPoint: String,
    val events: List<String>
)

@Serializable
data class VideoSdkCreateRoomBody(
    val webhook: VideoSdkWebhookBody
)

class Call {

    private var _token: String? = null
    private var videoSdkEndpoint = "https://api.videosdk.live/v2"

    private val http = HttpClient(Java) {
        engine {
            protocolVersion = HttpClient.Version.HTTP_2
        }
        install(ContentNegotiation) {
            expectSuccess = true
            json(json)
        }
    }

    fun start(scope: CoroutineScope) {
        scope.launch {
            while (Thread.currentThread().isAlive) {
                try {
                    val token = jwt(1.hours)

                    _token = token
                    delay(1.hours.inWholeSeconds.seconds.minus(30.seconds))
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                    delay(15.seconds)
                }
            }
        }
    }

    suspend fun updateParticipantCount(roomId: String, sessionId: String) {
        updateParticipantCount(roomId, activeSessionParticipants(sessionId))
    }

    fun updateParticipantCount(roomId: String, session: VideoSdkSessionsResponse, push: Boolean = true) {
        db.callByRoom(
            roomId
        )?.let { call ->
            call.participants = session.pageInfo?.total ?: 0

            db.update(call)

            if (push) {
                notify.callStatus(
                    db.document(Group::class, call.group!!)!!,
                    call
                )
            }
        }
    }

    fun jwt(expiresIn: Duration) = JWT.create()
        .withAudience(videoSdkEndpoint)
        .withPayload(json.encodeToString(VideoSdkJwtPayload(secrets.videoSdk.apiKey)))
        .withIssuedAt(Clock.System.now().toJavaInstant().toGMTDate().toJvmDate())
        .withExpiresAt(Clock.System.now().plus(expiresIn).toJavaInstant().toGMTDate().toJvmDate())
        .sign(Algorithm.HMAC256(secrets.videoSdk.secret))

    suspend fun createRoom() = http.post("$videoSdkEndpoint/rooms") {
        header(HttpHeaders.Authorization, _token ?: throw IllegalStateException("VideoSDK: Missing JWT"))
        contentType(ContentType.Application.Json.withCharset(UTF_8))
        setBody(
            VideoSdkCreateRoomBody(
                webhook = VideoSdkWebhookBody(
                    endPoint = "https://${secrets.config.domain}/videosdk/webhook",
                    events = listOf(
                        "participant-joined",
                        "participant-left",
                        "session-started",
                        "session-ended"
                    )
                )
            )
        )
    }.run {
        Logger.getAnonymousLogger().warning("VideoSDK (/rooms): " + bodyAsText())
        body<VideoSdkRoomResponse>()
    }

    suspend fun validateRoom(roomId: String) =
        get<VideoSdkRoomResponse>("rooms/validate/$roomId")

    suspend fun activeRoomSession(roomId: String) =
        get<VideoSdkSessionsResponse>("sessions?roomId=$roomId&page=1&perPage=1")

    suspend fun activeSessionParticipants(sessionId: String) =
        get<VideoSdkSessionsResponse>("sessions/$sessionId/participants/active?page=1&perPage=1")

    private suspend inline fun <reified T> get(url: String) = http.get("$videoSdkEndpoint/$url"){
        header(HttpHeaders.Authorization, _token ?: throw IllegalStateException("VideoSDK: Missing JWT"))
        contentType(ContentType.Application.Json.withCharset(UTF_8))
    }.run {
        Logger.getAnonymousLogger().warning("VideoSDK ($url): " + bodyAsText())
        body<T>()
    }
}
