package app.ailaai.api

import com.queatz.db.Rating

suspend fun Api.ratings(
    offset: Int = 0,
    limit: Int = 20,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Rating>>,
) = get(
    url = "ratings",
    parameters = mapOf(
        "offset" to offset.toString(),
        "limit" to limit.toString()
    ),
    onError = onError,
    onSuccess = onSuccess
)
