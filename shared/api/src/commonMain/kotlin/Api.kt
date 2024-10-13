package app.ailaai.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import kotlinx.serialization.json.Json

typealias ErrorBlock = (suspend (Throwable) -> Unit)?
typealias SuccessBlock<T> = suspend (T) -> Unit

abstract class Api {

    abstract val baseUrl: String
    protected abstract var authToken: String?
    abstract val httpClient: HttpClient
    abstract val httpDataClient: HttpClient
    abstract val httpJson: Json

    protected abstract suspend fun showError(t: Throwable)

    internal suspend inline fun <reified T : Any> post(
        url: String,
        noinline progressCallback: ((Float) -> Unit)? = null,
        client: HttpClient = httpClient,
        noinline onError: ErrorBlock,
        noinline onSuccess: SuccessBlock<T>
    ) {
        post(url, null as String?, progressCallback, client, onError, onSuccess)
    }

    internal suspend inline fun <reified Body : Any, reified T : Any> post(
        url: String,
        body: Body?,
        noinline progressCallback: ((Float) -> Unit)? = null,
        client: HttpClient = httpClient,
        noinline onError: ErrorBlock,
        noinline onSuccess: SuccessBlock<T>
    ) {
        try {
            onSuccess(post(url, body, progressCallback, client))
        } catch (t: Throwable) {
            if (onError?.invoke(t) == null) {
                showError(t)
            }
        }
    }

    internal suspend inline fun <reified R : Any> post(
        url: String,
        noinline progressCallback: ((Float) -> Unit)? = null,
        client: HttpClient = httpClient,
    ): R = post(url, null as String?, progressCallback, client)

    internal suspend inline fun <reified R : Any, reified T : Any> post(
        url: String,
        body: T?,
        noinline progressCallback: ((Float) -> Unit)? = null,
        client: HttpClient = httpClient,
    ): R = client.post("$baseUrl/${url}") {
        onUpload { bytesSentTotal, contentLength ->
            val contentLength = contentLength ?: 0L
            val progress =
                if (contentLength > 0L) (bytesSentTotal.toDouble() / contentLength.toDouble()).toFloat() else 0f
            progressCallback?.invoke(progress)
        }

        if (authToken != null) {
            bearerAuth(authToken!!)
        }

        if (client == httpClient) {
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
        }

        setBody(body)
    }.body()

    internal suspend inline fun <reified T: Any> get(
        url: String,
        parameters: Map<String, String?>? = null,
        client: HttpClient = httpClient,
        noinline onError: ErrorBlock,
        noinline onSuccess: SuccessBlock<T>
    ) {
        try {
            onSuccess(get(url, parameters, client))
        } catch (t: Throwable) {
            if (onError?.invoke(t) == null) {
                showError(t)
            }
        }
    }

    internal suspend inline fun <reified T : Any> get(
        url: String,
        parameters: Map<String, String?>? = null,
        client: HttpClient = httpClient,
    ): T = client.get("$baseUrl/${url}") {
        if (authToken != null) {
            bearerAuth(authToken!!)
        }

        if (client == httpClient) {
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
        }

        parameters?.filter { it.value != null }?.forEach { (key: String, value) -> parameter(key, value) }
    }.body()
}
