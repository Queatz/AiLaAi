package app.ailaai.api

import com.queatz.db.AppFeedback
import com.queatz.db.AppStats
import com.queatz.db.Search
import com.queatz.db.StatsHealth

suspend fun Api.stats(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<AppStats> = {}
) = get("/stats", onError = onError, onSuccess = onSuccess)

suspend fun Api.statsHealth(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<StatsHealth> = {}
) = get("/stats/health", onError = onError, onSuccess = onSuccess)

suspend fun Api.statsSearches(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Search> = {}
) = get("/stats/searches", onError = onError, onSuccess = onSuccess)

suspend fun Api.statsFeedback(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<AppFeedback>> = {}
) = get("/stats/feedback", onError = onError, onSuccess = onSuccess)
