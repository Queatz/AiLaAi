package com.queatz.ailaai.api

import com.queatz.ailaai.data.*
import io.ktor.http.*

suspend fun Api.confirmLinkDeviceToken(
    token: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post("link-device/$token/confirm", onError = onError, onSuccess = onSuccess)
