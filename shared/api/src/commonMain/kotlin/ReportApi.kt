package app.ailaai.api


import com.queatz.db.Report
import io.ktor.http.*

suspend fun Api.sendReport(
    report: Report,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("report", report, onError = onError, onSuccess = onSuccess)

suspend fun Api.reports(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Report>> = {}
) = get("report", onError = onError, onSuccess = onSuccess)
