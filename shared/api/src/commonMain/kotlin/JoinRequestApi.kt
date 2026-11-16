package app.ailaai.api

import com.queatz.db.JoinRequest
import com.queatz.db.JoinRequestAndPerson
import io.ktor.client.statement.*

suspend fun Api.joinRequests(
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<List<JoinRequestAndPerson>> = {}
) = get(
    url = "join-requests",
    errorContext = "load join requests",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.myJoinRequests(
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<List<JoinRequestAndPerson>> = {}
) = get(
    url = "me/join-requests",
    errorContext = "load join requests",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.newJoinRequest(
    joinRequest: JoinRequest,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<HttpResponse> = {}
) = post(
    url = "join-requests",
    body = joinRequest,
    errorContext = "send join request",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.acceptJoinRequest(
    joinRequest: String,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<HttpResponse> = {}
) = post(
    url = "join-requests/$joinRequest/accept",
    errorContext = "accept join request",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteJoinRequest(
    joinRequest: String,
    onError: ErrorBlock = {},
    onSuccess: SuccessBlock<HttpResponse> = {}
) = post(
    url = "join-requests/$joinRequest/delete",
    errorContext = "delete join request",
    onError = onError,
    onSuccess = onSuccess
)
