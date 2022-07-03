package com.queatz.ailaai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.parsing.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.Instant
import java.lang.reflect.Type
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

val api = Api()

class Api {

    private lateinit var context: Context

    private val baseUrl = "http://10.0.2.2:8080"

    private val tokenKey = stringPreferencesKey("token")

    private val http = HttpClient {
        expectSuccess = true

        install(ContentNegotiation) {
            gson {
                registerTypeAdapter(Instant::class.java, InstantTypeConverter())
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 10.seconds.inWholeMilliseconds
        }
    }

    private var token: String? = null

    fun init(context: Context) {
        this.context = context

        runBlocking {
            token = context.dataStore.data.first()[tokenKey]
        }
    }

    private suspend inline fun <reified T : Any> post(url: String, body: Any? = null): T = http.post("$baseUrl/${url}") {
        if (token != null) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        contentType(ContentType.Application.Json)

        if (body != null) {
            setBody(body)
        }
    }.body()

    private suspend inline fun <reified T : Any> get(url: String): T = http.get("$baseUrl/${url}") {
        if (token != null) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        accept(ContentType.Application.Json)
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

    suspend fun signUp(code: String): TokenResponse = post("sign/up", SignUpRequest(code))

    suspend fun myCards(): List<Card> = get("me/cards")

    suspend fun newCard(): Card = post("cards")

    suspend fun updateCard(id: String, card: Card): Card = post("cards/${id}", card)
}

data class SignUpRequest(
    val code: String
)

data class SignOnRequest(
    val email: String,
    val code: String?
)

data class TokenResponse(
    val token: String
)

class InstantTypeConverter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(
        src: Instant,
        srcType: Type,
        context: JsonSerializationContext
    ) = JsonPrimitive(DateTimeFormatter.ISO_INSTANT.format(src.toJavaInstant()))

    override fun deserialize(
        json: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ) = try {
        json.asString.toInstant()
    } catch (e: ParseException) {
        null
    }
}

