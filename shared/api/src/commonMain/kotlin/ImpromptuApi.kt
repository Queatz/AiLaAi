package app.ailaai.api

import com.queatz.db.*
import io.ktor.http.HttpStatusCode

suspend fun Api.myImpromptu(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Impromptu>,
) = get(
    url = "me/impromptu",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateMyImpromptu(
    impromptu: Impromptu,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Impromptu> = {},
) = post(
    url = "me/impromptu",
    body = impromptu,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createImpromptuSeek(
    impromptuSeek: ImpromptuSeek,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<ImpromptuSeek> = {},
) = post(
    url = "me/impromptu/seek",
    body = impromptuSeek,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateImpromptuSeek(
    id: String,
    impromptuSeek: ImpromptuSeek,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<ImpromptuSeek> = {},
) = post(
    url = "me/impromptu/seek/$id",
    body = impromptuSeek,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteImpromptuSeek(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {},
) = post(
    url = "me/impromptu/seek/$id/delete",
    onError = onError,
    onSuccess = onSuccess
)
