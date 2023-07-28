package com.queatz.ailaai.api

import com.queatz.ailaai.data.Api
import com.queatz.ailaai.data.ErrorBlock
import com.queatz.ailaai.data.Message
import com.queatz.ailaai.data.SuccessBlock
import io.ktor.http.*

suspend fun Api.message(
    message: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Message>,
) = get(
    "messages/$message",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteMessage(
    message: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post(
    "messages/$message/delete",
    onError = onError,
    onSuccess = onSuccess
)
