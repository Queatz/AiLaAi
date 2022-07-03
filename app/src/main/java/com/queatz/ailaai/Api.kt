package com.queatz.ailaai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

val api = Api()

class Api {

    private lateinit var context: Context

    private val baseUrl = "http://10.0.2.2:8080"

    private val tokenKey = stringPreferencesKey("token")

    private val http = HttpClient {
        expectSuccess = true

        install(ContentNegotiation) {
            gson()
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

    suspend fun signUp(code: String): TokenResponse = post("sign/up", SignUpRequest(code))

    private suspend inline fun <reified T : Any> post(url: String, body: Any): T = http.post("$baseUrl/${url}") {
        if (token != null) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        contentType(ContentType.Application.Json)
        setBody(body)
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
