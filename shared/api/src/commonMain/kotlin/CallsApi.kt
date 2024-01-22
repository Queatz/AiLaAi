package app.ailaai.api

import com.queatz.db.Call

suspend fun Api.calls(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Call>>,
) = get("calls", onError = onError, onSuccess = onSuccess)
