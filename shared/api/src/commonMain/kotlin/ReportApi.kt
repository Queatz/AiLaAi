package app.ailaai.api


import com.queatz.db.Report
import io.ktor.http.*

suspend fun Api.sendReport(
    report: Report,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("report", report, errorContext = "send report", onError = onError, onSuccess = onSuccess)

suspend fun Api.reports(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Report>> = {}
) = get("report", errorContext = "load reports", onError = onError, onSuccess = onSuccess)

suspend fun Api.resolveReport(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("report/$id/resolve", errorContext = "resolve report", onError = onError, onSuccess = onSuccess)
