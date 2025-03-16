package app.ailaai.api

import com.queatz.db.App
import com.queatz.db.AppDetailsBody

suspend fun Api.apps(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<App>>,
) = get(
    url = "apps",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createApp(
    details: AppDetailsBody,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<App>,
) = post(
    url = "apps",
    body = details,
    onError = onError,
    onSuccess = onSuccess
)
