package com.queatz.ailaai.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.queatz.ailaai.DeviceType
import com.queatz.ailaai.dataStore
import com.queatz.ailaai.extensions.showDidntWork
import io.ktor.client.*
import io.ktor.client.call.*
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.seconds

val json = Json {
    encodeDefaults = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    ignoreUnknownKeys = true
}

val api = Api()

const val appDomain = "https://ailaai.app"

typealias ErrorBlock = (suspend (Exception) -> Unit)?
typealias SuccessBlock<T> = suspend (T) -> Unit

class Api {

    private val _onUnauthorized = MutableSharedFlow<Unit>()
    val onUnauthorized = _onUnauthorized.asSharedFlow()

    internal lateinit var context: Context

    internal val baseUrl = "https://api.ailaai.app"
//    private val baseUrl = "http://10.0.2.2:8080"

    private val tokenKey = stringPreferencesKey("token")

    private val http = HttpClient {
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

    private val httpData = HttpClient {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }
    }

    private var token: String? = null

    fun init(context: Context) {
        this.context = context

        runBlocking {
            token = context.dataStore.data.first()[tokenKey]
        }
    }

    internal fun client() = http
    internal fun dataClient() = httpData

    fun signOut() {
        setToken(null)
    }

    fun url(it: String) = "$baseUrl$it"

    internal suspend inline fun <reified T : Any> post(
        url: String,
        noinline progressCallback: ((Float) -> Unit)? = null,
        client: HttpClient = http,
        noinline onError: ErrorBlock,
        noinline onSuccess: SuccessBlock<T>
    ) {
        post(url, null as String?, progressCallback, client, onError, onSuccess)
    }

    internal suspend inline fun <reified Body : Any, reified T : Any> post(
        url: String,
        body: Body?,
        noinline progressCallback: ((Float) -> Unit)? = null,
        client: HttpClient = http,
        noinline onError: ErrorBlock,
        noinline onSuccess: SuccessBlock<T>
    ) {
        try {
            onSuccess(post(url, body, progressCallback, client))
        } catch (e: Exception) {
            e.printStackTrace()
            if (onError?.invoke(e) == null ) {
                // Usually cancellations are from the user leaving the page
                if (e !is CancellationException && e !is InterruptedException) {
                    withContext(Dispatchers.Main) {
                        context.showDidntWork()
                    }
                }
            }
        }
    }

    internal suspend inline fun <reified R : Any> post(
        url: String,
        noinline progressCallback: ((Float) -> Unit)? = null,
        client: HttpClient = http,
    ): R = post(url, null as String?, progressCallback, client)

    internal suspend inline fun <reified R : Any, reified T : Any> post(
        url: String,
        body: T?,
        noinline progressCallback: ((Float) -> Unit)? = null,
        client: HttpClient = http,
    ): R = client.post("$baseUrl/${url}") {
        onUpload { bytesSentTotal, contentLength ->
            val progress =
                if (contentLength > 0) (bytesSentTotal.toDouble() / contentLength.toDouble()).toFloat() else 0f
            progressCallback?.invoke(progress)
        }

        if (token != null) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (client == http) {
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
        }

        setBody(body)
    }.body()

    internal suspend inline fun <reified T: Any> get(
        url: String,
        parameters: Map<String, String>? = null,
        client: HttpClient = http,
        noinline onError: ErrorBlock,
        noinline onSuccess: SuccessBlock<T>
    ) {
        try {
            onSuccess(get(url, parameters, client))
        } catch (e: Exception) {
            e.printStackTrace()
            // Usually cancellations are from the user leaving the page
            if (onError?.invoke(e) == null ) {
                if (e !is CancellationException && e !is InterruptedException) {
                    withContext(Dispatchers.Main) {
                        context.showDidntWork()
                    }
                }
            }
        }
    }

    internal suspend inline fun <reified T : Any> get(
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

        parameters?.forEach { (key: String, value) -> parameter(key, value) }
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

//    suspend fun latestAppVersion() = httpData.get("$appDomain/latest").bodyAsText().trim().toIntOrNull()

    suspend fun latestAppVersionInfo() = httpData.get("$appDomain/version-info").bodyAsText().trim().split(",").let {
        VersionInfo(
            versionCode = it.first().toInt(),
            versionName = it[1]
        )
    }

    suspend fun appReleaseNotes() = httpData.get("$appDomain/release-notes").bodyAsText()

    suspend fun downloadFile(url: String, outputStream: FileOutputStream) {
        httpData.get(url).bodyAsChannel().copyTo(outputStream)
    }
}

@Serializable
data class VersionInfo(
    val versionCode: Int,
    val versionName: String
)

@Serializable
data class SignUpRequest(
    val code: String?
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
    val cardCount: Int,
)

@Serializable
data class PersonProfile(
    val person: Person,
    val profile: Profile,
    val stats: ProfileStats,
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
data class ExportDataResponse(
    val profile: Profile? = null,
    val cards: List<Card>? = null,
    val stories: List<Story>? = null
)
