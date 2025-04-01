package app.ailaai.api

import com.queatz.db.Invite
import com.queatz.db.UseInviteResponse
import io.ktor.http.HttpStatusCode

suspend fun Api.createQuickInvite(
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Invite> = {}
) = get(
    url = "invite",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.createInvite(
    invite: Invite,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Invite> = {}
) = post(
    url = "invite",
    body = invite,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.invite(
    code: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Invite>
) = get(
    url = "invite/$code",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.updateInvite(
    id: String,
    invite: Invite,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<Invite> = {}
) = post(
    url = "invite/$id",
    body = invite,
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.useInvite(
    code: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<UseInviteResponse> = {}
) = post(
    url = "invite/$code/use",
    onError = onError,
    onSuccess = onSuccess
)

suspend fun Api.deleteInvite(
    id: String,
    onError: ErrorBlock = null,
    onSuccess: SuccessBlock<HttpStatusCode> = {}
) = post(
    url = "invite/$id/delete",
    onError = onError,
    onSuccess = onSuccess
)
