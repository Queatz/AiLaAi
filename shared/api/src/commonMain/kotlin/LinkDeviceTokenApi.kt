package app.ailaai.api

import io.ktor.http.*

suspend fun Api.confirmLinkDeviceToken(
    token: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post("link-device/$token/confirm", errorContext = "link device", onError = onError, onSuccess = onSuccess)
