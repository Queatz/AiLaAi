package app.ailaai.api

import com.queatz.db.Crash
import io.ktor.http.*

suspend fun Api.crash(
    report: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("crash", Crash(report), onError = onError, onSuccess = onSuccess)

suspend fun Api.crashes(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Crash>>
) = get("crash", onError = onError, onSuccess =  onSuccess)
