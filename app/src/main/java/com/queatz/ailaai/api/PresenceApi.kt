package com.queatz.ailaai.api

import com.queatz.ailaai.Api
import com.queatz.ailaai.ErrorBlock
import com.queatz.ailaai.Presence
import com.queatz.ailaai.SuccessBlock
import io.ktor.http.*

suspend fun Api.presence(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Presence>
) = get("me/presence", onError = onError, onSuccess = onSuccess)

suspend fun Api.readStoriesUntilNow(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("me/presence/read-stories", onError = onError, onSuccess = onSuccess)
