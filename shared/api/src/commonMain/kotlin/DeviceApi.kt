import app.ailaai.api.Api
import app.ailaai.api.ErrorBlock
import app.ailaai.api.SuccessBlock
import com.queatz.db.LinkDeviceToken


suspend fun Api.linkDevice(
    token: String,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<LinkDeviceToken> = {}
) = get(
    url = "link-device/$token",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.linkDevice(
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<LinkDeviceToken> = {}
) = post(
    url = "link-device",
    onError = onError,
    onSuccess = onSuccess
)
