package com.queatz.ailaai.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.showDidntWork
import com.queatz.db.VersionInfo
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.observer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.seconds

val json = Json {
    encodeDefaults = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    ignoreUnknownKeys = true
    explicitNulls = false
}

val api = Api()

const val appDomain = "https://ailaai.app"

class Api : app.ailaai.api.Api() {

    private val _onUnauthorized = MutableSharedFlow<Unit>()
    val onUnauthorized = _onUnauthorized.asSharedFlow()

    internal lateinit var context: Context

    override val baseUrl = "https://api.ailaai.app"
//    private val baseUrl = "http://10.0.2.2:8080"

    private val tokenKey = stringPreferencesKey("token")

    override val httpClient = HttpClient {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30.seconds.inWholeMilliseconds
        }

        install(ResponseObserver) {
            onResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized) {
                    _onUnauthorized.emit(Unit)
                }
            }
        }
    }

    override val httpDataClient = HttpClient {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 120.seconds.inWholeMilliseconds
        }
    }

    override val httpJson: Json get() = json

    override var authToken: String? = null

    fun init(context: Context) {
        this.context = context

        runBlocking {
            authToken = context.dataStore.data.first()[tokenKey]
        }
    }

    fun signOut() {
        setToken(null)
    }

    fun url(it: String) = "$baseUrl$it"

    override suspend fun showError(t: Throwable) {
        t.printStackTrace()
        // Usually cancellations are from the user leaving the page
        if (t !is CancellationException && t !is InterruptedException) {
            withContext(Dispatchers.Main) {
                context.showDidntWork()
            }
        }
    }

    fun setToken(token: String?) {
        this.authToken = token

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

    fun hasToken() = authToken != null

//    suspend fun latestAppVersion() = httpData.get("$appDomain/latest").bodyAsText().trim().toIntOrNull()

    suspend fun latestAppVersionInfo() = httpDataClient.get("$appDomain/version-info").bodyAsText().trim().split(",").let {
        VersionInfo(
            versionCode = it.first().toInt(),
            versionName = it[1]
        )
    }

    suspend fun appReleaseNotes() = httpDataClient.get("$appDomain/release-notes").bodyAsText()

    suspend fun downloadFile(url: String, outputStream: FileOutputStream) {
        httpDataClient.get(url).bodyAsChannel().copyTo(outputStream)
    }
}
