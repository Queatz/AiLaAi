package com.queatz.ailaai.api

import com.queatz.ailaai.data.Api
import com.queatz.ailaai.data.ErrorBlock
import com.queatz.ailaai.data.Presence
import com.queatz.ailaai.data.SuccessBlock
import io.ktor.http.*

suspend fun Api.presence(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Presence>
) = get("me/presence", onError = onError, onSuccess = onSuccess)

suspend fun Api.readStoriesUntilNow(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("me/presence/read-stories", onError = onError, onSuccess = onSuccess)
