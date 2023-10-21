package com.queatz.ailaai.api

import com.queatz.ailaai.data.Api
import com.queatz.ailaai.data.ErrorBlock
import com.queatz.ailaai.data.SuccessBlock
import com.queatz.db.Report
import io.ktor.http.*

suspend fun Api.sendReport(
    report: Report,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("report", report, onError = onError, onSuccess = onSuccess)
