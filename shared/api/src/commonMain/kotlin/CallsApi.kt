package app.ailaai.api

import com.queatz.db.Call

suspend fun Api.calls(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Call>>,
) = get("calls", onError = onError, onSuccess = onSuccess)

suspend fun Api.call(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Call>,
) = get("calls/$id", onError = onError, onSuccess = onSuccess)
