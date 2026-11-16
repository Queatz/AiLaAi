package app.ailaai.api

import com.queatz.db.PersonStatus
import com.queatz.db.Status
import io.ktor.client.utils.EmptyContent.status
import io.ktor.http.HttpStatusCode

suspend fun Api.myStatus(
    personStatus: PersonStatus,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<PersonStatus>,
) = post("me/status", personStatus, errorContext = "update status", onError = onError, onSuccess = onSuccess)

suspend fun Api.recentStatuses(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<Status>>,
) = get("me/statuses", errorContext = "load statuses", onError = onError, onSuccess = onSuccess)

suspend fun Api.createStatus(
    status: Status,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Status>,
) = post("statuses", status, errorContext = "create status", onError = onError, onSuccess = onSuccess)

suspend fun Api.friendStatuses(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<List<PersonStatus>>,
) = get("statuses", errorContext = "load statuses", onError = onError, onSuccess = onSuccess)

suspend fun Api.deleteStatus(
    statusId: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode>,
) = post("statuses/$statusId/delete", errorContext = "delete status", onError = onError, onSuccess = onSuccess)
