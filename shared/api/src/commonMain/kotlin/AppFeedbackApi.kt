package app.ailaai.api

import com.queatz.db.AppFeedback
import io.ktor.http.*

suspend fun Api.sendAppFeedback(
    appFeedback: AppFeedback,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("feedback", appFeedback, errorContext = "send feedback", onError = onError, onSuccess = onSuccess)
