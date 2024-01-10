package com.queatz

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.queatz.db.*
import com.queatz.plugins.db
import com.queatz.plugins.json
import com.queatz.plugins.secrets
import com.queatz.push.CollaborationPushData
import com.queatz.push.JoinRequestPushData
import com.queatz.push.MessagePushData
import com.queatz.push.PushData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.util.*
import io.ktor.util.date.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.logging.Logger
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class Push {

    private val events = MutableSharedFlow<Pair<PushData, Device>>()

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    companion object {
        private val hmsOAuthEndpoint = "https://oauth-login.cloud.huawei.com/oauth2/v3/token"
        private val gmsOAuthEndpoint = "https://oauth2.googleapis.com/token"
        private val hmsPushEndpoint = "https://push-api.cloud.huawei.com/v1/${secrets.hms.appId}/messages:send"
        private val gmsPushEndpoint = "https://fcm.googleapis.com/v1/projects/${secrets.gms.appId}/messages:send"
        private val apnsPushEndpoint = "https://api.push.apple.com:443"
    }

    private var hmsToken: String? = null
    private var gmsToken: String? = null
    private var apnsToken: String? = null

    private lateinit var coroutineScope: CoroutineScope

    fun start(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
        this.coroutineScope.launch {
            start()
        }
    }

    private suspend fun start() {
        withContext(Dispatchers.IO) {
            // HMS token
            launch {
                while (Thread.currentThread().isAlive) {
                    try {
                        val response = http.post(hmsOAuthEndpoint) {
                            header("Host", "oauth-login.cloud.huawei.com")
                            contentType(ContentType.Application.FormUrlEncoded)
                            setBody(FormDataContent(Parameters.build {
                                append("grant_type", "client_credentials")
                                append("client_id", secrets.hms.clientId)
                                append("client_secret", secrets.hms.clientSecret)
                            }))
                        }

                        Logger.getAnonymousLogger().info(response.toString())

                        if (response.status.isSuccess()) {
                            response.body<OAuthResponse>().let {
                                hmsToken = it.access_token
                                delay((it.expires_in ?: 1.hours.inWholeSeconds).seconds.minus(30.seconds))
                            }
                        } else {
                            delay(1.minutes)
                        }
                    } catch (throwable: Throwable) {
                        throwable.printStackTrace()
                        delay(15.seconds)
                    }
                }
            }

            // GMS token
            launch {
                while (Thread.currentThread().isAlive) {
                    try {
                        val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(secrets.gms.privateKey))
                        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)
                        val token = JWT.create()
                            .withAudience(gmsOAuthEndpoint)
                            .withIssuer(secrets.gms.clientEmail)
                            .withKeyId(secrets.gms.privateKeyId)
                            .withSubject(secrets.gms.clientEmail)
                            .withIssuedAt(Clock.System.now().toJavaInstant().toGMTDate().toJvmDate())
                            .withExpiresAt(Clock.System.now().plus(1.hours).toJavaInstant().toGMTDate().toJvmDate())
                            .withClaim("scope", "https://www.googleapis.com/auth/firebase.messaging")
                            .withClaim("type", "service_account")
                            .sign(Algorithm.RSA256(null, privateKey as RSAPrivateKey))

                        val response = http.post(gmsOAuthEndpoint) { // https://oauth2.googleapis.com/token ??
                            contentType(ContentType.Application.FormUrlEncoded)
                            setBody(FormDataContent(Parameters.build {
                                append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                                append("assertion", token)
                            }))
                        }

                        Logger.getAnonymousLogger().info(response.toString())

                        if (response.status.isSuccess()) {
                            response.body<OAuthResponse>().let {
                                gmsToken = it.access_token
                                delay((it.expires_in ?: 1.hours.inWholeSeconds).seconds.minus(30.seconds))
                            }
                        } else {
                            delay(1.minutes)
                        }
                    } catch (throwable: Throwable) {
                        throwable.printStackTrace()
                        delay(15.seconds)
                    }
                }
            }

            // APNS token
            launch {
                while (Thread.currentThread().isAlive) {
                    try {
                        val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(secrets.apns.privateKey))
                        val privateKey = KeyFactory.getInstance("ES256").generatePrivate(keySpecPKCS8)
                        val token = JWT.create()
                            .withAudience(apnsPushEndpoint)
                            .withIssuer(secrets.apns.teamId)
                            .withKeyId(secrets.apns.privateKeyId)
                            .withSubject(secrets.apns.teamId)
                            .withIssuedAt(Clock.System.now().toJavaInstant().toGMTDate().toJvmDate())
                            .withExpiresAt(Clock.System.now().plus(1.hours).toJavaInstant().toGMTDate().toJvmDate())
                            .sign(Algorithm.RSA256(null, privateKey as RSAPrivateKey))

                        apnsToken = token
                        delay(1.hours.inWholeSeconds.seconds.minus(30.seconds))
                    } catch (throwable: Throwable) {
                        throwable.printStackTrace()
                        delay(15.seconds)
                    }
                }
            }
        }
    }

    fun sendPush(device: Device, pushData: PushData) {
        coroutineScope.launch(Dispatchers.IO) {
            // Todo: Add to queue, retry, persist, etc.
            doSendPush(device, pushData)
        }
    }

    fun flow(device: String) = events
        .filter { it.second.id == device }
        .map { it.first }

    private suspend fun doSendPush(device: Device, pushData: PushData) {
        Logger.getAnonymousLogger().info("Sending push to ${json.encodeToString(device)}:")
        Logger.getAnonymousLogger().info(json.encodeToString(pushData))
        when (device.type!!) {
            DeviceType.Hms -> {
                try {
                    val response = http.post(hmsPushEndpoint) {
                        contentType(ContentType.Application.Json)
                        header(HttpHeaders.Authorization, "Bearer $hmsToken")
                        setBody(
                            HmsPushBody(
                                HmsPushBodyMessage(
                                    data = json.encodeToString(pushData),
                                    token = listOf(device.token!!)
                                )
                            )
                        )
                    }
                    Logger.getAnonymousLogger().info(response.bodyAsText())
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }

            DeviceType.Gms -> {
                try {
                    val response = http.post(gmsPushEndpoint) {
                        contentType(ContentType.Application.Json)
                        header(HttpHeaders.Authorization, "Bearer $gmsToken")
                        setBody(
                            GmsPushBody(
                                GmsPushBodyMessage(
                                    data = mapOf(
                                        "action" to pushData.action!!.name,
                                        "data" to json.encodeToString(
                                            when (val it = pushData.data) {
                                                is CollaborationPushData -> it
                                                is MessagePushData -> it
                                                is JoinRequestPushData -> it
                                                else -> error("Unknown push data type")
                                            }
                                        )
                                    ),
                                    token = device.token!!
                                )
                            )
                        )
                    }
                    if (response.status == HttpStatusCode.NotFound) {
                        onDeviceUnregistered(device)
                    }
                    Logger.getAnonymousLogger().info("FCM response: ${response.status} ${response.bodyAsText()}")
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
            DeviceType.Web -> {
                events.emit(pushData to device)
            }
            DeviceType.Apns -> {
                try {
                    val response = http.post("$apnsPushEndpoint/3/device/${device.token!!}") {
                        contentType(ContentType.Application.Json)
                        header(HttpHeaders.Authorization, "Bearer $apnsToken")
                        header("apns-push-type", "alert")
                        setBody(
                            HmsPushBody(
                                HmsPushBodyMessage(
                                    data = json.encodeToString(pushData),
                                    token = listOf(device.token!!)
                                )
                            )
                        )
                    }
                    Logger.getAnonymousLogger().info(response.bodyAsText())
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
        }
    }

    private fun onDeviceUnregistered(device: Device) {
        db.deleteDevice(device.type!!, device.token!!)
    }
}

@Serializable
data class OAuthResponse(
    val access_token: String? = null,
    val expires_in: Long? = null,
)

@Serializable
data class HmsPushBody(
    var message: HmsPushBodyMessage? = HmsPushBodyMessage(),
)

@Serializable
data class HmsPushBodyMessage(
    var data: String? = null,
    var token: List<String>? = null,
)

@Serializable
data class GmsPushBody(
    var message: GmsPushBodyMessage? = GmsPushBodyMessage(),
)

@Serializable
data class GmsPushBodyMessage(
    var data: Map<String, String>? = null,
    var token: String? = null,
)
