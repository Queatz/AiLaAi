package app.ailaai.api

import com.queatz.db.Geo

suspend fun Api.categories(
    geo: Geo,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<String>>,
) = get(
    "categories",
    mapOf(
        "geo" to geo.toString()
    ),
    onError = onError,
    onSuccess = onSuccess
)
