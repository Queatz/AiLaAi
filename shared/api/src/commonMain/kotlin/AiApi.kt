import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.db.AiPhotoRequest
import com.queatz.db.AiPhotoResponse
import com.queatz.db.AiSpeakRequest
import io.ktor.http.content.ByteArrayContent

suspend fun Api.aiStyles(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Pair<String, String>>>
) = get("ai/styles", onError = onError, onSuccess = onSuccess)

suspend fun Api.aiPhoto(
    request: AiPhotoRequest,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<AiPhotoResponse>
) = post("ai/photo", request, onError = onError, onSuccess = onSuccess)

suspend fun Api.aiSpeak(
    request: AiSpeakRequest,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<ByteArray>
) = post("ai/speak", request, onError = onError, onSuccess = onSuccess)
