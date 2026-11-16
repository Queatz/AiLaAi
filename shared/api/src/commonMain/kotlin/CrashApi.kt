package app.ailaai.api

import com.queatz.db.Crash
import io.ktor.http.*

suspend fun Api.crash(
    report: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("crash", Crash(report), errorContext = "send crash report", onError = onError, onSuccess = onSuccess)

suspend fun Api.crashes(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Crash>>
) = get("crash", errorContext = "load crash reports", onError = onError, onSuccess =  onSuccess)

suspend fun Api.resolveCrash(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("crash/$id/resolve", errorContext = "resolve crash", onError = onError, onSuccess = onSuccess)
