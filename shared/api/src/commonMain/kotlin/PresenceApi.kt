package app.ailaai.api

import com.queatz.db.Presence
import io.ktor.http.*

suspend fun Api.presence(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Presence>
) = get("me/presence", errorContext = "load presence", onError = onError, onSuccess = onSuccess)

suspend fun Api.readStoriesUntilNow(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post("me/presence/read-stories", errorContext = "mark posts read", onError = onError, onSuccess = onSuccess)
