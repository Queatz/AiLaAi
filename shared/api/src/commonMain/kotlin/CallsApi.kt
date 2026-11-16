package app.ailaai.api

import com.queatz.db.Call

suspend fun Api.calls(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Call>>,
) = get("calls", errorContext = "load calls", onError = onError, onSuccess = onSuccess)

suspend fun Api.call(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Call>,
) = get("calls/$id", errorContext = "load call", onError = onError, onSuccess = onSuccess)
