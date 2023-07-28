package com.queatz.ailaai.api

import com.queatz.ailaai.data.Api
import com.queatz.ailaai.data.Crash
import com.queatz.ailaai.data.ErrorBlock
import com.queatz.ailaai.data.SuccessBlock
import io.ktor.http.*

suspend fun Api.crash(
    report: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("crash", Crash(report), onError = onError, onSuccess = onSuccess)
