import io.ktor.client.*
import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set
import baseUrl as apiBaseUrl

val api = Api()

class Api : app.ailaai.api.Api() {
    val device: String
        get() {
            val device = localStorage["device"]
            return if (device.isNullOrBlank()) {
                (0 until 128).token().also {
                    localStorage["device"] = it
                }
            } else {
                device
            }
        }
    override val baseUrl: String = apiBaseUrl
    override var authToken: String? get() = application.bearerToken.value
        set(value) {
            application.bearerToken.value = value
        }
    override val httpClient: HttpClient = http
    override val httpDataClient: HttpClient = http
    override val httpJson: Json = json

    override suspend fun showError(t: Throwable) {}
}
