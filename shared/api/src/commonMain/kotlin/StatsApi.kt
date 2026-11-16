package app.ailaai.api

import com.queatz.db.AppFeedback
import com.queatz.db.AppStats
import com.queatz.db.StatsHealth
import io.ktor.http.HttpStatusCode

suspend fun Api.stats(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<AppStats> = {}
) = get("/stats", errorContext = "load stats", onError = onError, onSuccess = onSuccess)

suspend fun Api.statsHealth(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<StatsHealth> = {}
) = get("/stats/health", errorContext = "load health", onError = onError, onSuccess = onSuccess)

suspend fun Api.statsFeedback(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<AppFeedback>> = {}
) = get("/stats/feedback", errorContext = "load feedback", onError = onError, onSuccess = onSuccess)

suspend fun Api.resolveFeedback(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("/stats/feedback/$id/resolve", errorContext = "resolve feedback", onError = onError, onSuccess = onSuccess)
