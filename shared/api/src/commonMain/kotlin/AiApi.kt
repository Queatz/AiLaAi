import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.db.AiPhotoRequest
import com.queatz.db.AiPhotoResponse

suspend fun Api.aiStyles(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Pair<String, String>>>
) = get("ai/styles", onError = onError, onSuccess = onSuccess)

suspend fun Api.aiPhoto(
    request: AiPhotoRequest,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<AiPhotoResponse>
) = post("ai/photo", request, onError = onError, onSuccess = onSuccess)
