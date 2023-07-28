package com.queatz.ailaai.api

import com.queatz.ailaai.data.Api
import com.queatz.ailaai.data.AppFeedback
import com.queatz.ailaai.data.ErrorBlock
import com.queatz.ailaai.data.SuccessBlock
import io.ktor.http.*

suspend fun Api.sendAppFeedback(
    appFeedback: AppFeedback,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("feedback", appFeedback, onError = onError, onSuccess = onSuccess)
