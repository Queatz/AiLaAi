package com.queatz.ailaai.api

import com.queatz.ailaai.data.Api
import com.queatz.ailaai.data.ErrorBlock
import com.queatz.ailaai.data.SuccessBlock
import io.ktor.http.*

suspend fun Api.confirmLinkDeviceToken(
    token: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post("link-device/$token/confirm", onError = onError, onSuccess = onSuccess)
