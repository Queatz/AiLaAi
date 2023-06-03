package com.queatz.ailaai.api

import com.queatz.ailaai.Api
import com.queatz.ailaai.Crash
import com.queatz.ailaai.ErrorBlock
import com.queatz.ailaai.SuccessBlock
import io.ktor.http.*

suspend fun Api.crash(
    report: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("crash", Crash(report), onError = onError, onSuccess = onSuccess)
