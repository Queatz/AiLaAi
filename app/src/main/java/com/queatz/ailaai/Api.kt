package com.queatz.ailaai

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import at.bluesource.choicesdk.maps.common.LatLng
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.observer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

val json = Json {
    encodeDefaults = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    ignoreUnknownKeys = true
}

val api = Api()

const val appDomain = "https://ailaai.app"

class Api {

    private val _onUnauthorized = MutableSharedFlow<Unit>()
    val onUnauthorized = _onUnauthorized.asSharedFlow()

    private lateinit var context: Context

    private val baseUrl = "https://api.ailaai.app"
//    private val baseUrl = "http://10.0.2.2:8080"

    private val tokenKey = stringPreferencesKey("token")

    private val http = HttpClient {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 10.seconds.inWholeMilliseconds
        }

        install(ResponseObserver) {
            onResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized) {
                    _onUnauthorized.emit(Unit)
                }
            }
        }
    }

    private val httpData = HttpClient {
        expectSuccess = true
    }

    private var token: String? = null

    fun init(context: Context) {
        this.context = context

        runBlocking {
            token = context.dataStore.data.first()[tokenKey]
        }
    }

    fun signout() {
        setToken(null)
    }

    fun url(it: String) = "$baseUrl$it"

    private suspend inline fun <reified R : Any> post(
        url: String,
        client: HttpClient = http,
    ): R = post(url, null as String?, client)

    private suspend inline fun <reified R : Any, reified T : Any> post(
        url: String,
        body: T?,
        client: HttpClient = http,
    ): R = client.post("$baseUrl/${url}") {
        if (token != null) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (client == http) {
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
        }

        setBody(body)
    }.body()

    private suspend inline fun <reified T : Any> get(
        url: String,
        parameters: Map<String, String>? = null,
        client: HttpClient = http,
    ): T = client.get("$baseUrl/${url}") {
        if (token != null) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (client == http) {
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
        }

        parameters?.forEach { (key, value) -> parameter(key, value) }
    }.body()

    fun setToken(token: String?) {
        this.token = token

        CoroutineScope(Dispatchers.Default).launch {
            context.dataStore.edit {
                if (token == null) {
                    it.remove(tokenKey)
                } else {
                    it[tokenKey] = token
                }
            }
        }
    }

    fun hasToken() = token != null

    suspend fun signUp(inviteCode: String): TokenResponse = post("sign/up", SignUpRequest(inviteCode))

    suspend fun signIn(transferCode: String): TokenResponse = post("sign/in", SignInRequest(transferCode))

    suspend fun me(): Person = get("me")

    suspend fun transferCode(): Transfer = get("me/transfer")

    suspend fun myDevice(deviceType: DeviceType, deviceToken: String): HttpStatusCode =
        post("me/device", Device(deviceType, deviceToken))

    suspend fun updateMe(person: Person): Person = post("me", person)

    suspend fun people(search: String): List<Person> = get("people", mapOf("search" to search))

    suspend fun profile(personId: String): PersonProfile = get("people/$personId/profile")

    suspend fun updateProfile(profile: Profile): HttpStatusCode = post("me/profile", profile)

    suspend fun updateProfilePhoto(photo: Uri): HttpStatusCode = post("me/profile/photo", MultiPartFormDataContent(
        formData {
            append("photo", photo.asScaledJpeg(context), Headers.build {
                append(HttpHeaders.ContentType, "image/jpg")
                append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
            })
        }
    ), client = httpData)

    suspend fun cards(geo: LatLng, offset: Int = 0, limit: Int = 20, search: String? = null): List<Card> = get("cards", mapOf(
        "geo" to "${geo.latitude},${geo.longitude}",
        "offset" to offset.toString(),
        "limit" to limit.toString()
    ) + (search?.let {
        mapOf("search" to search)
    } ?: mapOf()))

    suspend fun categories(geo: LatLng): List<String> = get(
        "categories",
        mapOf(
            "geo" to "${geo.latitude},${geo.longitude}"
        )
    )

    suspend fun card(id: String): Card = get("cards/$id")

    suspend fun cardsCards(id: String): List<Card> = get("cards/$id/cards")

    suspend fun profileCards(personId: String): List<Card> = get("people/$personId/profile/cards")

    suspend fun myCards(): List<Card> = get("me/cards")

    suspend fun myCollaborations(): List<Card> = get("me/collaborations")
    suspend fun leaveCollaboration(card: String): HttpStatusCode =
        post("me/collaborations/leave", LeaveCollaborationBody(card))

    suspend fun savedCards(search: String? = null): List<SaveAndCard> = get("me/saved", search?.let {
        mapOf("search" to search)
    } ?: mapOf())

    suspend fun newCard(card: Card? = Card(offline = true)): Card = post("cards", card)

    suspend fun updateCard(id: String, card: Card): Card = post("cards/$id", card)

    suspend fun deleteCard(id: String): HttpStatusCode = post("cards/$id/delete")

    suspend fun saveCard(id: String): HttpStatusCode = post("cards/$id/save")

    suspend fun unsaveCard(id: String): HttpStatusCode = post("cards/$id/unsave")

    suspend fun invite(): Invite = get("invite")

    suspend fun updateMyPhoto(photo: Uri): HttpStatusCode = post("me/photo", MultiPartFormDataContent(
        formData {
            append("photo", photo.asScaledJpeg(context), Headers.build {
                append(HttpHeaders.ContentType, "image/jpg")
                append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
            })
        }
    ), client = httpData)

    suspend fun uploadCardPhoto(id: String, photo: Uri): HttpStatusCode =
        post("cards/$id/photo", MultiPartFormDataContent(
            formData {
                append("photo", photo.asScaledJpeg(context), Headers.build {
                    append(HttpHeaders.ContentType, "image/jpg")
                    append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
                })
            }
        ), client = httpData)

    suspend fun cardGroup(card: String): Group = get("cards/$card/group")

    suspend fun cardPeople(card: String): List<Person> = get("cards/$card/people")

    suspend fun createGroup(people: List<String>, reuse: Boolean = false): Group =
        post("groups", CreateGroupBody(people.toSet().toList(), reuse))

    suspend fun updateGroup(id: String, groupUpdate: Group): Group = post("groups/$id", groupUpdate)

    suspend fun groups(): List<GroupExtended> = get("groups")

    suspend fun group(id: String): GroupExtended = get("groups/$id")

    suspend fun createMember(member: Member): Member = post("members", member)

    suspend fun updateMember(id: String, member: Member): HttpStatusCode = post("members/$id", member)

    suspend fun removeMember(id: String): HttpStatusCode = post("members/$id/delete")

    suspend fun messages(group: String): List<Message> = get("groups/$group/messages")

    suspend fun messagesBefore(group: String, before: Instant): List<Message> = get(
        "groups/$group/messages",
        mapOf(
            "before" to before.toString()
        )
    )

    suspend fun sendMessage(group: String, message: Message): HttpStatusCode = post("groups/$group/messages", message)

    suspend fun sendPhotos(group: String, photos: List<Uri>): HttpStatusCode =
        post("groups/$group/photos", MultiPartFormDataContent(
            formData {
                photos.forEachIndexed { index, photo ->
                    append("photo[$index]", photo.asScaledJpeg(context), Headers.build {
                        append(HttpHeaders.ContentType, "image/jpg")
                        append(HttpHeaders.ContentDisposition, "filename=photo.jpg")
                    })
                }
            }
        ), client = httpData)

    suspend fun deleteMessage(message: String): HttpStatusCode = post("messages/$message/delete")

    suspend fun latestAppVersion() = httpData.get("$appDomain/latest").bodyAsText().trim().toIntOrNull()
}

@Serializable
data class SignUpRequest(
    val code: String,
)

@Serializable
data class SignInRequest(
    val code: String,
)

@Serializable
data class TokenResponse(
    val token: String,
)

@Serializable
class GroupExtended(
    var group: Group? = null,
    var members: List<MemberAndPerson>? = null,
    var latestMessage: Message? = null,
)

@Serializable
class MemberAndPerson(
    var person: Person? = null,
    var member: Member? = null,
)

@Serializable
data class ProfileStats(
    val friendsCount: Int,
    val cardCount: Int
)

@Serializable
data class PersonProfile(
    val person: Person,
    val profile: Profile,
    val stats: ProfileStats
)

@Serializable
class SaveAndCard(
    var save: Save? = null,
    var card: Card? = null,
)

@Serializable
class Device(
    val type: DeviceType,
    val token: String,
)

@Serializable
private data class CreateGroupBody(val people: List<String>, val reuse: Boolean)

@Serializable
private data class LeaveCollaborationBody(val card: String)
