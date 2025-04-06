import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.db.AiPhotoRequest
import com.queatz.db.AiPhotoResponse
import com.queatz.db.AiScriptRequest
import com.queatz.db.AiScriptResponse
import com.queatz.db.AiSpeakRequest

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
