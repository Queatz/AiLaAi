package com.queatz.ailaai.api

import com.queatz.ailaai.Api
import com.queatz.ailaai.ErrorBlock
import com.queatz.ailaai.Report
import com.queatz.ailaai.SuccessBlock
import io.ktor.http.*

suspend fun Api.sendReport(
    report: Report,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("report", report, onError = onError, onSuccess = onSuccess)
