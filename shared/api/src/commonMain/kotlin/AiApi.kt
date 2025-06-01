import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.db.AiJsonRequest
import com.queatz.db.AiJsonResponse
import com.queatz.db.AiPhotoRequest
import com.queatz.db.AiPhotoResponse
import com.queatz.db.AiScriptRequest
import com.queatz.db.AiScriptResponse
import com.queatz.db.AiSpeakRequest
import com.queatz.db.AiTranscribeResponse
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

suspend fun Api.aiStyles(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Pair<String, String>>>,
) = get(
    url = "ai/styles",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.aiPhoto(
    request: AiPhotoRequest,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<AiPhotoResponse>,
) = post(
    url = "ai/photo",
    body = request,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.aiSpeak(
    request: AiSpeakRequest,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<ByteArray>,
) = post(
    url = "ai/speak",
    body = request,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.aiScript(
    request: AiScriptRequest,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<AiScriptResponse>,
) = post(
    url = "ai/script",
    body = request,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.aiTranscribe(
    audio: ByteArray,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<AiTranscribeResponse>,
) = post(
    url = "ai/transcribe",
    body = MultiPartFormDataContent(
        formData {
            append(
                key = "audio",
                value = audio,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, "audio/webm")
                    append(HttpHeaders.ContentDisposition, "filename=audio.webm")
                }
            )
        }
    ),
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.aiJson(
    request: AiJsonRequest,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<AiJsonResponse>,
) = post(
    url = "ai/json",
    body = request,
    onError = onError,
    onSuccess = onSuccess
)
